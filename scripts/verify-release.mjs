/** Compare uploaded metadata and hashes against the locally boot-tested release. */
import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const version = fs.readFileSync(path.join(root, "VERSION"), "utf8").trim();
const directory = path.join(root, "all-jars");
const results = JSON.parse(fs.readFileSync(path.join(directory, `publish-results-${version}.json`), "utf8"));
const envPath = path.join(process.env.USERPROFILE, "NightBeam-Knowledge-Base/secrets/local.env");
const env = Object.fromEntries(fs.readFileSync(envPath, "utf8").split(/\r?\n/)
  .filter(line => line.includes("=") && !line.startsWith("#"))
  .map(line => { const index = line.indexOf("="); return [line.slice(0, index).trim(), line.slice(index + 1).trim().replace(/^"|"$/g, "")]; }));

async function json(url, headers = {}) {
  const response = await fetch(url, { headers });
  if (!response.ok) throw new Error(`Verification HTTP ${response.status}`);
  return response.json();
}

if (results.length !== 18 || new Set(results.map(row => `${row.platform}:${row.jar}`)).size !== 18) {
  throw new Error("Expected nine uploaded jars on each platform");
}
const checks = [];
for (const row of results) {
  const bytes = fs.readFileSync(path.join(directory, row.jar));
  if (createHash("sha512").update(bytes).digest("hex") !== row.sha512) throw new Error(`Local hash changed: ${row.jar}`);
  const [, loader, game] = row.jar.match(/^smartvillagers-(fabric|forge|neoforge)-(.+)-\d+\.\d+\.\d+\.jar$/);
  if (row.platform === "modrinth") {
    const release = await json(`https://api.modrinth.com/v2/version/${row.id}`);
    if (release.version_number !== `${version}+${game}-${loader}` || release.project_id !== "l9oZCCPS"
      || !release.loaders.includes(loader) || !release.game_versions.includes(game)
      || !release.files.some(file => file.filename === row.jar && file.hashes.sha512 === row.sha512)
      || (loader === "fabric" && !release.dependencies.some(dep => dep.project_id === "P7dR8mSH" && dep.dependency_type === "required"))) {
      throw new Error(`Modrinth metadata or hash mismatch: ${row.jar}`);
    }
    checks.push({ ...row, status: release.status, verified: true });
  } else {
    const { data: file } = await json(`https://api.curseforge.com/v1/mods/1567718/files/${row.id}`,
      { "x-api-key": env.CURSEFORGE_API_KEY });
    if (file.modId !== 1567718 || file.fileName !== row.jar || !file.gameVersions.includes(game)
      || !file.gameVersions.some(value => value.toLowerCase() === loader)
      || (loader === "fabric" && !file.dependencies.some(dep => dep.modId === 306612 && dep.relationType === 3))) {
      throw new Error(`CurseForge metadata mismatch: ${row.jar}`);
    }
    const sha1 = createHash("sha1").update(bytes).digest("hex");
    const publishedHash = file.hashes?.find(hash => hash.algo === 1)?.value;
    if (publishedHash && publishedHash !== sha1) throw new Error(`CurseForge hash mismatch: ${row.jar}`);
    checks.push({ ...row, status: file.fileStatus, available: file.isAvailable,
      hashVerified: !!publishedHash, verified: true });
  }
}
fs.writeFileSync(path.join(directory, `remote-verification-${version}.json`), JSON.stringify(checks, null, 2));
console.log(JSON.stringify(checks.map(row => ({ platform: row.platform, jar: row.jar, id: row.id,
  status: row.status, available: row.available, hashVerified: row.hashVerified, verified: row.verified })), null, 2));
