#!/usr/bin/env python3
"""Writes the mod's data files (advancements and the English language file) into every workspace, each in the
format of its Minecraft version. Edit the definitions here, not the generated JSON.

  python3 tools/datagen.py

Format differences handled here:
  1.20.1   plural folder (advancements), {"item": id} icons, texture-path backgrounds
  1.21.1   singular folder (advancement), {"id": id} icons, texture-path backgrounds
  26.x     asset-id advancement backgrounds
The advancements use a single "minecraft:impossible" criterion named "done"; the mod grants them in code
(platform/Mc.award), so no criterion syntax differs between versions.
"""
from __future__ import annotations

import json
import shutil
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
NS = 'smartvillagers'
VERSIONS = ['1.20.1', '1.21.1', '26.1.2', '26.2', '26.3']

# id: (parent, icon item, title, description, frame, experience)
ADVANCEMENTS = {
    'root': (None, 'emerald', 'Smart Villagers', 'Say something to a villager standing nearby', 'task', 0),
    'first_request': ('root', 'writable_book', 'A Favor for a Friend',
                      'Ask a villager for work, accept their request and finish it', 'task', 0),
    'village_hero': ('first_request', 'bell', 'Hero of the Village Square',
                     'Finish 10 requests for villagers', 'challenge', 100),
}

LANG: dict = {
}


def text(key, fallback):
    return {'translate': key, 'fallback': fallback}


def advancement(version, name, spec, lang):
    parent, icon, title, desc, frame, xp = spec
    legacy = version == '1.20.1'
    lang[f'advancements.{NS}.{name}.title'] = title
    lang[f'advancements.{NS}.{name}.description'] = desc
    display = {
        'icon': {'item': f'minecraft:{icon}'} if legacy else {'id': f'minecraft:{icon}'},
        'title': text(f'advancements.{NS}.{name}.title', title),
        'description': text(f'advancements.{NS}.{name}.description', desc),
        'frame': frame,
        'show_toast': True,
        'announce_to_chat': frame != 'task',
    }
    if parent is None:
        display['background'] = ('minecraft:block/emerald_block' if version.startswith('26.')
                                 else 'minecraft:textures/block/emerald_block.png')
    out = {'display': display, 'criteria': {'done': {'trigger': 'minecraft:impossible'}},
           'requirements': [['done']]}
    if parent:
        out = {'parent': f'{NS}:{parent}', **out}
    if xp:
        out['rewards'] = {'experience': xp}
    return out


def main():
    for version in VERSIONS:
        res = REPO / version / 'common' / 'src' / 'main' / 'resources'
        if not res.parent.exists():
            continue
        folder = 'advancements' if version == '1.20.1' else 'advancement'
        data = res / 'data' / NS
        for old in ('advancements', 'advancement'):
            shutil.rmtree(data / old, ignore_errors=True)
        lang = dict(LANG)
        for name, spec in ADVANCEMENTS.items():
            path = data / folder / f'{name}.json'
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(advancement(version, name, spec, lang), indent=2) + '\n')
        lang_file = res / 'assets' / NS / 'lang' / 'en_us.json'
        lang_file.parent.mkdir(parents=True, exist_ok=True)
        lang_file.write_text(json.dumps(dict(sorted(lang.items())), indent=2, ensure_ascii=False) + '\n')
        print(f'{version}: {len(ADVANCEMENTS)} advancements, {len(lang)} lang keys')


if __name__ == '__main__':
    main()
