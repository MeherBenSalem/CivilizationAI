#!/usr/bin/env python3
"""Copies the shared common code from the reference workspace (1.21.1) to the other Minecraft versions, rewriting
the few names that moved between versions. Version-specific files are listed in KEEP and never overwritten; put
anything that differs between versions behind platform/Mc.java (one copy per workspace) instead.

  python3 tools/sync_common.py [workspace ...]      (default: 1.20.1 26.1.2 26.2 26.3)
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
REFERENCE = '1.21.1'
PKG = 'common/src/{}/java/tn/naizo/smartvillagers'

# Relative to the package root; these differ per version and stay as each workspace has them.
KEEP_ALL = {
    'platform/Mc.java',
    'platform/Services.java',
    'mixin/VillagerAiDataMixin.java',
    'villager/PersonaOverride.java',
    'villager/VillagerMemory.java',
    'villager/VillagerPersona.java',
    'voice/VoiceOutput.java',
}
KEEP_TESTS = {'voice/VoiceStartupLogTest.java'}
KEEP = {
    '1.20.1': {'platform/Mc.java'},
    '26.1.2': KEEP_ALL,
    '26.2': KEEP_ALL,
    '26.3': KEEP_ALL,
}


def rewrite(version: str, text: str) -> str:
    if version.startswith('26.'):
        text = text.replace('import net.minecraft.world.entity.npc.Villager;',
                            'import net.minecraft.world.entity.npc.villager.Villager;')
    return text


def sync(version: str):
    for kind, keep in (('main', KEEP[version]), ('test', KEEP_TESTS)):
        src = REPO / REFERENCE / PKG.format(kind)
        dst = REPO / version / PKG.format(kind)
        # Drop files that no longer exist in the reference (unless kept).
        for path in dst.rglob('*.java'):
            rel = path.relative_to(dst).as_posix()
            if rel not in keep and not (src / rel).exists():
                path.unlink()
        for path in src.rglob('*.java'):
            rel = path.relative_to(src).as_posix()
            target = dst / rel
            if rel in keep and target.exists():
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(rewrite(version, path.read_text()))
    # Mixin list: same mixins, each version's Java compatibility level.
    ref_json = (REPO / REFERENCE / 'common/src/main/resources/smartvillagers.mixins.json').read_text()
    level = 'JAVA_17' if version == '1.20.1' else 'JAVA_21'
    dst_json = REPO / version / 'common/src/main/resources/smartvillagers.mixins.json'
    dst_json.write_text(ref_json.replace('"JAVA_21"', f'"{level}"'))
    print(f'{version}: synced from {REFERENCE}')


def main():
    for version in sys.argv[1:] or ['1.20.1', '26.1.2', '26.2', '26.3']:
        sync(version)


if __name__ == '__main__':
    main()
