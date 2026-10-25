#!/usr/bin/env python3
"""Captures the marketing screenshots (marketing/images/screenshots/*.jpg) from a QA client in a real village.

Run against a 26.3 Fabric dev server on a default world (see marketing/README.md), with mock_ai.py running:

  python3 tools/qa/marketing_shots.py [name ...]

Every screenshot is a single unedited frame of the game window (1920x1080), saved as JPEG. What is staged, and
disclosed in marketing/README.md:
  * three working villagers (farmer, librarian, cleric) are summoned in the middle of a naturally generated plains
    village, facing the camera, and held still with NoAI (otherwise they walk out of frame);
  * the replies are the mod's built-in local dialogue (the mock AI provider is set to fail), except thinking.jpg,
    where the mock provider is slowed down so the "..." bubble can be photographed;
  * the time is set to morning and the weather to clear.
"""
from __future__ import annotations

import math
import re
import sys
import time
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from client import PLAYER, Session  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'marketing' / 'images' / 'screenshots'
MOCK = 'http://127.0.0.1:18080'


def mock(path):
    urllib.request.urlopen(MOCK + path, timeout=5).read()


class Shots:
    def __init__(self):
        self.s = Session()
        self.center = None
        self.villagers = []  # (tag, x, y, z)

    def cmd(self, c):
        return self.s.cmd(c)

    def save(self, name):
        from PIL import Image
        png = OUT / f'.{name}.png'
        self.s.shot(png)
        Image.open(png).convert('RGB').save(OUT / f'{name}.jpg', quality=90)
        png.unlink()
        print('saved', name)

    def ground(self, x, z):
        """The height of the ground at x, z: an invisible armor stand dropped from above lands on it."""
        self.cmd(f'summon minecraft:armor_stand {x} 140 {z} {{Invisible:1b,Tags:["mk.probe"]}}')
        y = None
        for _ in range(20):
            time.sleep(0.5)
            p = re.findall(r'(-?\d+\.\d+)d', self.cmd('data get entity @e[tag=mk.probe,limit=1] Pos'))
            if len(p) >= 3 and y is not None and abs(float(p[1]) - y) < 0.01:
                break
            y = float(p[1]) if len(p) >= 3 else None
        self.cmd('kill @e[tag=mk.probe]')
        return y

    def setup(self):
        for rule in ('doDaylightCycle false', 'advance_time false', 'doWeatherCycle false', 'advance_weather false',
                     'doMobSpawning false', 'spawn_mobs false', 'doInsomnia false'):
            self.cmd(f'gamerule {rule}')
        self.cmd('time set 4000')
        self.cmd('weather clear')
        out = self.cmd('locate structure minecraft:village_plains')
        m = re.search(r'\[(-?\d+), ~, (-?\d+)\]', out)
        if not m:
            raise SystemExit(f'no village: {out}')
        x, z = int(m.group(1)) + 0.5, int(m.group(2)) + 0.5
        self.cmd(f'forceload add {int(x) - 48} {int(z) - 48} {int(x) + 48} {int(z) + 48}')
        self.cmd(f'tp {PLAYER} {x} 130 {z}')
        time.sleep(20)  # terrain and villagers load
        self.center = (x, z)
        # Three working villagers in a row, facing south (towards the camera), held still for framing.
        self.cmd('kill @e[tag=mk.v]')
        found = []
        for i, (dx, dz, profession) in enumerate([(-2.4, 0.4, 'farmer'), (0.0, -0.6, 'librarian'), (2.4, 0.4, 'cleric')]):
            vx, vz = x + dx, z + dz
            vy = self.ground(vx, vz)
            tag = f'mk.v{i}'
            self.cmd(f'summon minecraft:villager {vx} {vy} {vz} {{NoAI:1b,PersistenceRequired:1b,Xp:60,Rotation:[0f,0f],'
                     f'VillagerData:{{profession:"minecraft:{profession}",level:3,type:"minecraft:plains"}},Tags:["mk.v","{tag}"]}}')
            found.append((tag, vx, vy, vz))
        # Re-roll a villager whose persona name repeats another's (names come from the UUID).
        for _ in range(10):
            names = [re.search(r'Persona for (\S+)', self.cmd(f'villagerai persona get @e[tag={t},limit=1]') or '')
                     for t, *_ in found]
            names = [m.group(1) if m else '' for m in names]
            dup = next((i for i, n in enumerate(names) if n and names.index(n) != i), None)
            if dup is None:
                break
            tag, vx, vy, vz = found[dup]
            profession = ['farmer', 'librarian', 'cleric'][dup]
            self.cmd(f'kill @e[tag={tag}]')
            self.cmd(f'summon minecraft:villager {vx} {vy} {vz} {{NoAI:1b,PersistenceRequired:1b,Xp:60,Rotation:[0f,0f],'
                     f'VillagerData:{{profession:"minecraft:{profession}",level:3,type:"minecraft:plains"}},Tags:["mk.v","{tag}"]}}')
        print('names', names)
        self.villagers = found
        self.ground_y = sum(v[2] for v in found) / len(found)
        print('villagers', found)
        mock('/qa/fail?on=1')
        mock('/qa/delay?s=0')
        time.sleep(2)

    def camera(self, back, up, aim_dx=0.0, aim_up=1.9, side=0.0, settle=0.4):
        """Stands the player `back` blocks south of the scene (and `side` east), `up` above the ground, looking at
        its centre."""
        x, z = self.center
        y = self.ground_y
        self.s.tp(x + side, y + up, z + back, facing=(x + aim_dx, y + aim_up, z))
        time.sleep(settle)

    def face(self, villager, back=3.4, settle=1.0):
        tag, vx, vy, vz = villager
        self.s.tp(vx, vy, vz + back, facing=(vx, vy + 1.9, vz))
        time.sleep(settle)

    def say(self, text, wait=2.0):
        self.s.chat(text)
        time.sleep(wait)

    def chorus(self):
        """All three villagers answer, one after the other, so their bubbles are up together."""
        # Questions get longer answers, so the first bubbles are still up when the last one appears.
        v = self.villagers
        for villager, line in zip([v[0], v[1], v[2]], ['Any news?', 'How is work?', 'Why so glum?']):
            self.face(villager, back=4.0, settle=0.5)
            self.say(line, 0.1)

    def run(self, names):
        OUT.mkdir(parents=True, exist_ok=True)
        self.setup()
        v = self.villagers
        self.say('/villagerai consent accept', 1)
        want = lambda name: not names or name in names  # noqa: E731

        if want('crowd'):
            self.chorus()
            self.s.hud(False)
            self.camera(back=6.0, up=0.6)
            self.save('crowd')
            self.s.hud(True)
            time.sleep(10)
        if want('square'):
            self.chorus()
            self.s.hud(False)
            self.camera(back=4.0, up=0.6, aim_up=1.8, side=-5.5)
            self.save('square')
            self.s.hud(True)
            time.sleep(10)

        if want('offer') or want('complete') or want('gossip'):
            self.face(v[0])
            self.say('Do you have any work for me?', 2.2)
            self.save('offer')
            self.say('Yes, I will do it!', 2)
            req = re.search(r'R [0-9a-f-]{36} r1;(FETCH|SLAY);([a-z_:]+);(\d+);',
                            self.cmd(f'data get entity @e[tag={v[0][0]},limit=1] smartvillagers.Village'))
            if req and req.group(1) == 'FETCH':
                self.cmd(f'give {PLAYER} {req.group(2)} {req.group(3)}')
            elif req:
                for _ in range(int(req.group(3))):
                    self.cmd(f'execute at {PLAYER} run summon {req.group(2)} ~ ~-40 ~ {{NoAI:1b,Tags:["mk.mob"]}}')
                    self.cmd(f'damage @e[tag=mk.mob,limit=1] 1000 minecraft:player_attack by {PLAYER}')
                self.cmd('kill @e[type=minecraft:item]')
            time.sleep(1)
            self.say('Here you go, all done!', 0.8)
            self.save('complete')
            time.sleep(8)
            self.face(v[1])
            self.say('Good afternoon!', 1.8)
            self.save('gossip')
            time.sleep(8)

        if want('thinking'):
            mock('/qa/fail?on=0')
            mock('/qa/delay?s=12')
            time.sleep(4)
            self.face(v[1], settle=2.5)
            self.say('What do you think of the weather?', 1.2)
            self.s.hud(False)
            time.sleep(0.5)
            self.save('thinking')
            self.s.hud(True)
            time.sleep(14)
            mock('/qa/delay?s=0')
            mock('/qa/fail?on=1')

        if want('advancements'):
            self.s.key('l')
            time.sleep(1.5)
            self.s.move(1880, 1040)  # no tooltip under the pointer
            time.sleep(1.0)
            self.save('advancements')
            self.s.key('Escape')


if __name__ == '__main__':
    Shots().run(sys.argv[1:])
