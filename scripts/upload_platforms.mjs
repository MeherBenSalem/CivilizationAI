/**
 * Upload Smart Villagers AI jars to Modrinth + CurseForge.
 * Prefers all-jars/ (multi-workspace buildAll), falls back to dist/.
 *
 * Usage:
 *   node scripts/upload_platforms.mjs
 *   node scripts/upload_platforms.mjs --dry-run
 *   node scripts/upload_platforms.mjs --modrinth-only
 *   node scripts/upload_platforms.mjs --curseforge-only
 */
import fs from "fs";
import path from "path";
import { createHash } from "node:crypto";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.join(__dirname, "..");

const MODRINTH_ID = "l9oZCCPS";
const CURSEFORGE_ID = "1567718";
const MOD_TITLE = "Smart Villagers AI";
const FABRIC_API_MODRINTH = "P7dR8mSH";
const VOICECHAT_MODRINTH = "9eGKb6K1";

function jarDir() {
  const allJars = path.join(ROOT, "all-jars");
  const dist = path.join(ROOT, "dist");
  if (fs.existsSync(allJars)) return allJars;
  if (fs.existsSync(dist)) return dist;
  throw new Error("Neither all-jars/ nor dist/ exists - run buildAll first");
}

function readGradleVersion() {
  const versionFile = path.join(ROOT, "VERSION");
  if (fs.existsSync(versionFile)) {
    return fs.readFileSync(versionFile, "utf8").trim();
  }
  for (const rel of ["1.21.1/gradle.properties", "gradle.properties"]) {
    const propsPath = path.join(ROOT, rel);
    if (!fs.existsSync(propsPath)) continue;
    const props = fs.readFileSync(propsPath, "utf8");
    const m = props.match(/^version=(.+)$/m);
    if (m) return m[1].trim();
  }
  throw new Error("VERSION / gradle.properties version= missing");
}

async function loadEnv() {
  const candidates = [
    path.join(process.env.USERPROFILE || "", "NightBeam-Knowledge-Base", "secrets", "local.env"),
    path.join(ROOT, "secrets", "local.env"),
  ];
  for (const envPath of candidates) {
    if (!fs.existsSync(envPath)) continue;
    const text = fs.readFileSync(envPath, "utf8");
    for (const line of text.split(/\r?\n/)) {
      const m = line.match(/^([^#=]+)=(.*)$/);
      if (!m) continue;
      const k = m[1].trim();
      const v = m[2].trim();
      if (!process.env[k]) process.env[k] = v;
    }
    console.log("Loaded secrets from", envPath);
    return;
  }
}

function parseArgs(argv) {
  const out = { curseforgeOnly: false, modrinthOnly: false, dryRun: false };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--curseforge-only") out.curseforgeOnly = true;
    else if (a === "--modrinth-only") out.modrinthOnly = true;
    else if (a === "--dry-run") out.dryRun = true;
    else throw new Error(`Unknown arg ${a}`);
  }
  return out;
}

function parseJar(filePath) {
  const name = path.basename(filePath);
  const m = name.match(/^(?:smartvillagers|Smart Villagers AI)-(fabric|forge|neoforge)-(.+)-(\d+\.\d+\.\d+)\.jar$/i);
  if (!m) throw new Error(`Cannot parse jar name: ${name}`);
  return { jar: filePath, name, loader: m[1].toLowerCase(), game: m[2], version: m[3] };
}

function dependenciesFor(loader) {
  const deps = [{ project_id: VOICECHAT_MODRINTH, dependency_type: "optional" }];
  if (loader === "fabric") {
    deps.unshift({ project_id: FABRIC_API_MODRINTH, dependency_type: "required" });
  }
  return deps;
}

function curseRelations(loader) {
  const projects = [{ projectID: 416089, slug: "simple-voice-chat", type: "optionalDependency" }];
  if (loader === "fabric") {
    projects.unshift({ projectID: 306612, slug: "fabric-api", type: "requiredDependency" });
  }
  return { projects };
}

async function uploadModrinthFile(file, changelog, token, dryRun, version) {
  // One jar = one Modrinth version (not multi-file / attached)
  const versionNumber = `${version}+${file.game}-${file.loader}`;
  const body = {
    name: `${MOD_TITLE} ${version} (${file.loader} ${file.game})`,
    version_number: versionNumber,
    changelog,
    dependencies: dependenciesFor(file.loader),
    game_versions: [file.game],
    version_type: "release",
    loaders: [file.loader],
    featured: false,
    status: "listed",
    project_id: MODRINTH_ID,
    file_parts: ["file_0"],
    primary_file: "file_0",
  };

  if (dryRun) {
    console.log("[dry-run] Modrinth", versionNumber, file.name);
    return;
  }

  let lastErr = null;
  for (let attempt = 1; attempt <= 5; attempt++) {
    const form = new FormData();
    form.append("data", JSON.stringify(body));
    form.append("file_0", new Blob([fs.readFileSync(file.jar)]), file.name);

    const res = await fetch("https://api.modrinth.com/v2/version", {
      method: "POST",
      headers: { Authorization: token },
      body: form,
    });
    const text = await res.text();
    if (res.ok) {
      const uploaded = JSON.parse(text);
      console.log("Modrinth OK", versionNumber, file.name, uploaded.id);
      return uploaded.id;
    }
    lastErr = new Error(`Modrinth ${res.status} ${versionNumber} ${text.slice(0, 800)}`);
    if ((res.status === 500 || res.status === 502 || res.status === 503 || res.status === 429) && attempt < 5) {
      console.warn("Modrinth", res.status, versionNumber, "- retry", attempt);
      await new Promise((r) => setTimeout(r, 10000 * attempt));
      continue;
    }
    throw lastErr;
  }
  throw lastErr;
}

async function main() {
  await loadEnv();
  const args = parseArgs(process.argv);
  const VERSION = readGradleVersion();

  const notesPath = path.join(ROOT, "PATCH_NOTES.md");
  const changelogPath = path.join(ROOT, "CHANGELOG.md");
  const changelog = fs.existsSync(notesPath)
    ? fs.readFileSync(notesPath, "utf8")
    : fs.existsSync(changelogPath)
      ? fs.readFileSync(changelogPath, "utf8")
      : `## ${MOD_TITLE} ${VERSION}\n\nRelease ${VERSION}.`;

  const DIST = jarDir();
  const jars = fs
    .readdirSync(DIST)
    .filter((f) => f.endsWith(".jar") && !f.includes("-sources") && !f.includes("-javadoc"))
    .filter((f) => /smartvillagers|Smart Villagers AI/i.test(f))
    .map((f) => parseJar(path.join(DIST, f)))
    .filter((j) => j.version === VERSION)
    .sort((a, b) => a.name.localeCompare(b.name));
  if (!jars.length) throw new Error(`No ${VERSION} smartvillagers jars in ${DIST}`);

  const expected = ["1.20.1-fabric", "1.20.1-forge", "1.21.1-fabric", "1.21.1-forge",
    "1.21.1-neoforge", "26.2-fabric", "26.2-neoforge", "26.3-fabric", "26.3-neoforge"].sort();
  if (JSON.stringify(jars.map(j => `${j.game}-${j.loader}`).sort()) !== JSON.stringify(expected)) {
    throw new Error("Release must contain exactly the nine supported Minecraft/loader jars");
  }
  const reportPath = path.join(DIST, `client-verification-${VERSION}.json`);
  if (!fs.existsSync(reportPath)) throw new Error("Run scripts/smoke-client.py before publishing");
  const report = JSON.parse(fs.readFileSync(reportPath, "utf8"));
  for (const jar of jars) {
    const hash = createHash("sha512").update(fs.readFileSync(jar.jar)).digest("hex");
    if (!report.some(row => row.jar === jar.name && row.passed && row.sha512 === hash
      && row.bootEvidence?.includes("httpContract=true"))) {
      throw new Error(`Missing client/HTTP verification for exact jar bytes: ${jar.name}`);
    }
  }
  const resultsPath = path.join(DIST, `publish-results-${VERSION}.json`);
  const results = fs.existsSync(resultsPath) ? JSON.parse(fs.readFileSync(resultsPath, "utf8")) : [];
  for (const row of results) {
    const jar = jars.find(item => item.name === row.jar);
    if (jar && row.sha512 !== createHash("sha512").update(fs.readFileSync(jar.jar)).digest("hex")) {
      throw new Error(`Jar changed after upload; use a new release version: ${row.jar}`);
    }
  }
  const record = (platform, jar, id) => {
    results.push({ platform, jar: jar.name, id,
      sha512: createHash("sha512").update(fs.readFileSync(jar.jar)).digest("hex") });
    fs.writeFileSync(resultsPath, JSON.stringify(results, null, 2));
  };

  const { MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY } = process.env;
  if (!MODRINTH_TOKEN || !CURSEFORGE_TOKEN || !CURSEFORGE_API_KEY) {
    throw new Error("Set MODRINTH_TOKEN, CURSEFORGE_TOKEN, CURSEFORGE_API_KEY");
  }

  console.log(`Uploading ${MOD_TITLE} ${VERSION} (${jars.length} jars)`);

  if (!args.curseforgeOnly) {
    for (const jar of jars) {
      if (!args.dryRun && results.some(row => row.platform === "modrinth" && row.jar === jar.name)) continue;
      const id = await uploadModrinthFile(jar, changelog, MODRINTH_TOKEN, args.dryRun, VERSION);
      if (!args.dryRun) record("modrinth", jar, id);
    }
  }

  if (args.modrinthOnly) return;

  const LOADER_IDS = { fabric: 7499, forge: 7498, neoforge: 10150 };
  const legacyRes = await fetch("https://minecraft.curseforge.com/api/game/versions", {
    headers: { "X-Api-Token": CURSEFORGE_TOKEN },
  });
  if (!legacyRes.ok) throw new Error(`CurseForge legacy versions API ${legacyRes.status}`);
  const legacyFlat = await legacyRes.json();
  const findLegacy = (want, preferType) => {
    const hits = legacyFlat.filter((v) => v.name === want);
    if (preferType != null) {
      const hit = hits.find((v) => v.gameVersionTypeID === preferType);
      if (!hit) throw new Error(`No CF legacy version for ${want} (type ${preferType})`);
      return hit.id;
    }
    if (!hits[0]) throw new Error(`No CF legacy version for ${want}`);
    return hits[0].id;
  };
  const clientId = findLegacy("Client");
  const serverId = findLegacy("Server");

  const resolveGameId = async (game) => {
    const verRes = await fetch(
      `https://api.curseforge.com/v1/minecraft/version/${encodeURIComponent(game)}`,
      { headers: { "x-api-key": CURSEFORGE_API_KEY, Accept: "application/json" } },
    );
    if (verRes.ok) {
      const verJson = await verRes.json();
      const gvId = verJson.data?.gameVersionId;
      if (gvId) {
        console.log("CF game", game, "-> gameVersionId", gvId);
        return gvId;
      }
    }
    const slug = game.replace(/\./g, "-");
    const candidates = legacyFlat.filter((v) => v.name === game || v.slug === slug);
    if (candidates[0]) {
      console.log("CF game", game, "-> legacy id", candidates[0].id);
      return candidates[0].id;
    }
    throw new Error(`No CF game version id for ${game}`);
  };

  for (const p of jars) {
    if (!args.dryRun && results.some(row => row.platform === "curseforge" && row.jar === p.name)) continue;
    const gameId = await resolveGameId(p.game);
    const loaderId = LOADER_IDS[p.loader];
    if (!loaderId) throw new Error(`Unknown loader for ${p.name}`);
    const meta = {
      changelog,
      changelogType: "markdown",
      displayName: p.name,
      gameVersions: [clientId, serverId, loaderId, gameId],
      releaseType: "release",
      relations: curseRelations(p.loader),
    };
    if (args.dryRun) {
      console.log("[dry-run] CurseForge", p.name, meta);
      continue;
    }
    const cfForm = new FormData();
    cfForm.append("metadata", JSON.stringify(meta));
    cfForm.append("file", new Blob([fs.readFileSync(p.jar)]), p.name);
    let ok = false;
    for (let attempt = 1; attempt <= 3; attempt++) {
      const cfRes = await fetch(
        `https://minecraft.curseforge.com/api/projects/${CURSEFORGE_ID}/upload-file`,
        { method: "POST", headers: { "X-Api-Token": CURSEFORGE_TOKEN }, body: cfForm },
      );
      const cfText = await cfRes.text();
      if (cfRes.ok) {
        console.log("CurseForge OK", p.name, cfText.slice(0, 120));
        record("curseforge", p, JSON.parse(cfText).id);
        ok = true;
        break;
      }
      if ((cfRes.status === 503 || cfRes.status === 429 || cfRes.status === 500) && attempt < 3) {
        console.warn("CurseForge", cfRes.status, "for", p.name, "- retry", attempt);
        await new Promise((r) => setTimeout(r, 15000 * attempt));
        continue;
      }
      throw new Error(`CurseForge ${cfRes.status} ${p.name} ${cfText.slice(0, 500)}`);
    }
    if (!ok) throw new Error(`CurseForge failed for ${p.name}`);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
