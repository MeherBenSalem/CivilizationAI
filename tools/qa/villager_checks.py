#!/usr/bin/env python3
"""In-game checks for Smart Villagers AI against a running dev server and QA client (see devrun.sh), with the mock
AI provider (mock_ai.py) on 127.0.0.1:18080. Everything the player says is typed into the real client's chat.

Covers: provider status, consent, an AI reply (and the prompt it was built from), the name tag, the speech bubble
riding the villager and expiring, a request offered/accepted/finished in chat (items or kills, emeralds, gossip,
advancement, ledger), news spreading to a neighbour (in the AI prompt and in local dialogue), local dialogue when the
provider fails, the clickable-button commands (assign, accept), refusal after hurting a villager, the request
cooldown, /villagerai requests, Hero of the Village Square after 10 requests, loading 1.x villager data, removal of
an orphaned bubble, and a clean server log.

  python3 tools/qa/villager_checks.py --log /tmp/sv-qa/fabric-server.log --client-log /tmp/sv-qa/fabric-client.log \\
      --world <server dir>/qa [--shots DIR]
"""
from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rcon import wait_for  # noqa: E402

NS = 'smartvillagers'
PLAYER = 'Builder'
MOCK = 'http://127.0.0.1:18080'
AI_LOG = Path('/tmp/sv-qa/ai-requests.jsonl')
Y = -60  # flat world surface
FAILURES: list[str] = []
REQUEST_RE = re.compile(r'R [0-9a-f-]{36} (r1;(FETCH|SLAY);([a-z_:]+);(\d+);(\d+);(\d+);([01]);(\d+))')
# Villagers: tag -> (profession, position)
VILLAGERS = {
    'qa.v1': ('farmer', (0.5, Y, 3.5)),
    'qa.v2': ('librarian', (0.5, Y, -3.5)),
    'qa.v3': ('weaponsmith', (7.5, Y, 0.5)),
}
LABEL = {'farmer': 'Farmer', 'librarian': 'Librarian', 'weaponsmith': 'Weaponsmith'}


def check(name, ok, detail=''):
    print(('PASS ' if ok else 'FAIL ') + name + (f'  [{str(detail)[:500]}]' if detail and not ok else ''), flush=True)
    if not ok:
        FAILURES.append(name)
    return ok


def mock(path):
    try:
        urllib.request.urlopen(MOCK + path, timeout=5).read()
    except OSError as e:
        print('mock control failed:', e)


def ai_requests(since=0):
    lines = AI_LOG.read_text().splitlines() if AI_LOG.exists() else []
    return [json.loads(line) for line in lines[since:]]


class Log:
    """A growing log file; wait() looks only at what was written after mark()."""

    def __init__(self, path):
        self.path = Path(path)
        self.offset = 0

    def mark(self):
        self.offset = self.path.stat().st_size if self.path.exists() else 0

    def new(self):
        if not self.path.exists():
            return ''
        with self.path.open('rb') as f:
            f.seek(self.offset)
            return f.read().decode('utf-8', 'replace')

    def wait(self, pattern, seconds=20):
        deadline = time.time() + seconds
        while time.time() < deadline:
            m = re.search(pattern, self.new())
            if m:
                return m
            time.sleep(0.5)
        return None


class Qa:
    def __init__(self, args):
        from client import Session
        self.port = args.port
        self.rcon = wait_for(args.port)
        self.rcon.sock.settimeout(30)
        self.session = Session(port=args.port)
        self.chat_log = Log(args.client_log)
        self.world = Path(args.world) if args.world else None
        self.shots = Path(args.shots) if args.shots else None
        self.names = {}

    def cmd(self, command):
        """One RCON command. Forge 1.21.1 never answers a command that produces no output at all, so a command
        that times out gets a fresh connection and counts as having printed nothing."""
        try:
            return self.rcon.command(command)
        except (TimeoutError, OSError):
            print(f'note: no RCON reply to "{command}"', flush=True)
            self.rcon = wait_for(self.port)
            self.rcon.sock.settimeout(30)
            return ''

    def run_as_player(self, command, pattern, seconds=15):
        """Types a /command into the client's chat (as the player) and waits for `pattern` in the client log."""
        return self.say('/' + command, pattern, seconds)

    def v(self, tag):
        return f'@e[type=minecraft:villager,tag={tag},limit=1]'

    def face(self, tag):
        x, y, z = VILLAGERS[tag][1]
        self.session.tp(0.5, Y, 0.5, facing=(x, y + 1.5, z))
        time.sleep(1.0)

    def say(self, text, pattern, seconds=25):
        """Types `text` into the client's chat and waits for `pattern` in the client log."""
        self.chat_log.mark()
        self.session.chat(text)
        return self.chat_log.wait(pattern, seconds)

    def village(self, tag):
        return self.cmd(f'data get entity {self.v(tag)} {NS}.Village')

    def request(self, tag):
        m = REQUEST_RE.search(self.village(tag))
        if not m:
            return None
        return {'kind': m.group(2), 'target': m.group(3), 'count': int(m.group(4)), 'reward': int(m.group(5)),
                'accepted': m.group(7) == '1'}

    def has_adv(self, path):
        out = self.cmd(f'execute if entity @a[name={PLAYER},advancements={{{NS}:{path}=true}}]')
        return 'passed' in out.lower()

    def emeralds(self):
        m = re.search(r'Found (\d+)', self.cmd(f'clear {PLAYER} minecraft:emerald 0'))
        return int(m.group(1)) if m else 0

    def carried(self, item):
        m = re.search(r'Found (\d+)', self.cmd(f'clear {PLAYER} {item} 0'))
        return int(m.group(1)) if m else 0

    def shot(self, name):
        if self.shots:
            self.session.shot(self.shots / f'{name}.png')

    def fulfil(self, request):
        """Gives the items, or summons and defeats the mobs, a request asks for."""
        if request['kind'] == 'FETCH':
            self.cmd(f'give {PLAYER} {request["target"]} {request["count"]}')
            return
        for i in range(request['count']):
            self.cmd(f'summon {request["target"]} {i % 5 - 2 + 0.5} {Y} 12.5 {{NoAI:1b,Tags:["qa.mob"]}}')
        time.sleep(1)
        for _ in range(request['count']):
            self.cmd(f'damage @e[type={request["target"]},tag=qa.mob,limit=1] 1000 minecraft:player_attack by {PLAYER}')
            time.sleep(0.3)
        time.sleep(1)
        self.cmd('kill @e[type=minecraft:item]')


def setup(qa):
    for rule in ('doDaylightCycle false', 'advance_time false', 'doMobSpawning false', 'spawn_mobs false',
                 'doWeatherCycle false', 'advance_weather false', 'doInsomnia false'):
        qa.cmd(f'gamerule {rule}')
    qa.cmd('time set noon')
    qa.cmd('weather clear')
    qa.cmd(f'op {PLAYER}')
    qa.cmd('forceload add -16 -16 16 16')
    qa.cmd('kill @e[type=!minecraft:player]')
    qa.cmd(f'fill -12 {Y} -12 12 {Y + 5} 12 minecraft:air')
    qa.session.tp(0.5, Y, 0.5)
    time.sleep(5)  # the client loads the terrain
    for tag, (profession, (x, y, z)) in VILLAGERS.items():
        out = qa.cmd(f'summon minecraft:villager {x} {y} {z} {{NoAI:1b,PersistenceRequired:1b,Xp:1,'
                     f'VillagerData:{{profession:"minecraft:{profession}",level:2,type:"minecraft:plains"}},'
                     f'Tags:["{tag}"]}}')
        check(f'setup: summon {profession}', 'Summoned' in out, out)
    time.sleep(2)


def run(qa):
    status = qa.cmd('villagerai status')
    check('status: provider is custom and ready', 'custom (ready)' in status, status)
    check('status: speech bubbles and requests on', 'Speech bubbles: on' in status and 'Requests: on' in status, status)

    m = qa.say('/villagerai consent accept', r'AI consent accepted')
    check('consent: accepted from the client', m is not None)

    # A reply through the AI provider.
    qa.face('qa.v1')
    since = len(ai_requests())
    m = qa.say('Hello there!', r'\[Farmer\] (\S+): QA-AI')
    if not check('ai: farmer replies through the provider', m is not None, qa.chat_log.new()[-800:]):
        return
    name1 = qa.names['qa.v1'] = m.group(1)
    sent = ai_requests(since)
    system = sent[-1]['system'] if sent else ''
    check('ai: prompt has persona and profession', f'You are {name1}' in system and 'farmer' in system, system)
    check('ai: prompt has biome and world facts', 'Biome:' in system and 'Daylight' in system, system)
    check('ai: prompt has the standing', 'Your attitude to this player: neutral' in system, system)
    check('ai: model and endpoint', sent and sent[-1]['model'] == 'qa-model' and sent[-1]['path'] == '/v1/chat/completions',
          sent[-1:])
    check('name tag: villager shows its persona name', name1 in qa.cmd(f'data get entity {qa.v("qa.v1")} CustomName'))
    check('advancement: Smart Villagers (root) on first talk', qa.has_adv('root'))

    # The speech bubble rides the villager and expires.
    passengers = qa.cmd(f'data get entity {qa.v("qa.v1")} Passengers')
    check('bubble: a text display rides the villager', 'smartvillagers.bubble' in passengers and 'text_display' in passengers,
          passengers)
    check('bubble: shows the reply', 'QA-AI' in passengers, passengers)
    qa.shot('bubble')
    gone = False
    for _ in range(45):
        time.sleep(1)
        if 'passed' not in qa.cmd('execute if entity @e[type=minecraft:text_display,tag=smartvillagers.bubble]').lower():
            gone = True
            break
    check('bubble: removed after a few seconds', gone)

    # A request offered, accepted and finished in chat.
    since = len(ai_requests())
    m = qa.say('Do you have any work for me?', rf'Request from {re.escape(name1)}: .*\[Accept\].*\[Decline\]')
    check('request: offer card with Accept and Decline', m is not None, qa.chat_log.new()[-800:])
    sent = ai_requests(since)
    check('request: the AI is told about the offer', sent and 'asking the player for a favour' in sent[-1]['system'],
          sent[-1:])
    request = qa.request('qa.v1')
    check('request: stored on the villager, not yet accepted', request is not None and not request['accepted'], qa.village('qa.v1'))
    if request is None:
        return
    qa.shot('offer')
    m = qa.say("Yes, I'll do it", r'Request accepted:')
    check('request: accepted by saying yes', m is not None, qa.chat_log.new()[-800:])
    request = qa.request('qa.v1')
    check('request: marked accepted on the villager', request is not None and request['accepted'], qa.village('qa.v1'))
    m = qa.say('How many do you still need?', rf'{re.escape(name1)}\'s request: .* 0/{request["count"]}')
    check('request: progress report 0/N', m is not None, qa.chat_log.new()[-800:])
    before = qa.emeralds()
    qa.fulfil(request)
    since = len(ai_requests())
    m = qa.say('Done, here you go!', r'Request complete!')
    check(f'request: {request["kind"].lower()} {request["count"]} {request["target"]} completed', m is not None,
          qa.chat_log.new()[-800:])
    sent = ai_requests(since)
    check('request: the AI is told it was finished', sent and 'just finished your request' in sent[-1]['system'], sent[-1:])
    check('request: emeralds paid', qa.emeralds() - before >= request['reward'], (before, qa.emeralds(), request))
    if request['kind'] == 'FETCH':
        check('request: items taken', qa.carried(request['target']) == 0, request)
    check('request: cleared from the villager', qa.request('qa.v1') is None, qa.village('qa.v1'))
    gossip = qa.cmd(f'data get entity {qa.v("qa.v1")} Gossips')
    check('request: positive gossip added', 'minor_positive' in gossip, gossip)
    check('advancement: A Favor for a Friend', qa.has_adv('first_request'))
    if qa.world:
        ledger = qa.world / NS / 'ledger.json'
        text = ledger.read_text() if ledger.exists() else ''
        check('ledger: one favour saved in the world', re.search(r'"[0-9a-f-]{36}": 1', text) is not None, text)
    qa.shot('complete')

    # News spreads to the neighbour.
    check('news: the librarian heard about it', 'H ' in qa.village('qa.v2'), qa.village('qa.v2'))
    qa.face('qa.v2')
    since = len(ai_requests())
    m = qa.say('Hello!', r'\[Librarian\] (\S+): QA-AI')
    check('news: librarian replies', m is not None, qa.chat_log.new()[-800:])
    if m:
        qa.names['qa.v2'] = m.group(1)
    sent = ai_requests(since)
    system = sent[-1]['system'] if sent else ''
    check('news: in the AI prompt', 'News you heard from neighbours' in system and f'for {name1} the farmer' in system, system)
    check('standing: friendly after a favour', 'Your attitude to this player: friendly' in system, system)

    # Local dialogue when the provider fails.
    mock('/qa/fail?on=1')
    time.sleep(4)  # the provider's per-player cooldown
    m = qa.say('Good morning!', r'\[Librarian\] \S+: Good to see you again, friend\. I heard you')
    check('fallback: local greeting with standing and news', m is not None, qa.chat_log.new()[-800:])

    # The clickable buttons' commands: an admin-assigned slay request, accepted with /villagerai request accept.
    name2 = qa.names.get('qa.v2', '')
    m = qa.run_as_player(f'villagerai request assign {qa.v("qa.v2")} slay', rf'Request from {re.escape(name2)}: Defeat')
    check('assign: slay request offered', m is not None, qa.chat_log.new()[-800:])
    m = qa.run_as_player('villagerai request accept', r'Request accepted: Defeat')
    check('buttons: /villagerai request accept', m is not None, qa.chat_log.new()[-800:])
    request = qa.request('qa.v2')
    if check('assign: slay request stored and accepted', request and request['kind'] == 'SLAY' and request['accepted'],
             qa.village('qa.v2')):
        qa.fulfil(request)
        m = qa.say('Done!', r'Request complete!')
        check(f'slay: {request["count"]} {request["target"]} defeated and paid', m is not None, qa.chat_log.new()[-800:])

    # Hurting a villager makes it refuse work.
    for _ in range(2):
        qa.cmd(f'damage {qa.v("qa.v3")} 1 minecraft:player_attack by {PLAYER}')
        time.sleep(1.2)
    qa.face('qa.v3')
    m = qa.say('Any work for me?', r'\[Weaponsmith\] \S+: After what you')
    check('refusal: hurt villager gives no work', m is not None, qa.chat_log.new()[-800:])

    # The same villager waits before the next request.
    qa.face('qa.v1')
    m = qa.say('Any more work for me?', rf'\[Farmer\] {re.escape(name1)}: You\'ve done plenty for me today')
    check('cooldown: no second request right away', m is not None, qa.chat_log.new()[-800:])

    m = qa.say('/villagerai requests', r'Village requests \(2 finished\)')
    check('requests: /villagerai requests lists the count', m is not None, qa.chat_log.new()[-800:])

    # Ten requests: Hero of the Village Square.
    for i in range(8):
        qa.run_as_player(f'villagerai request assign {qa.v("qa.v1")} fetch', r'Request from')
        qa.run_as_player('villagerai request accept', r'Request accepted')
        request = qa.request('qa.v1')
        if not request:
            check(f'hero: request {i + 3} assigned', False, qa.village('qa.v1'))
            break
        qa.fulfil(request)
        if not check(f'hero: request {i + 3} completed', qa.say('done', r'Request complete!') is not None,
                     qa.chat_log.new()[-600:]):
            break
    check('advancement: Hero of the Village Square after 10 requests', qa.has_adv('village_hero'))
    m = qa.say('Hello!', rf'\[Farmer\] {re.escape(name1)}: Our hero!')
    check('standing: hero greeting', m is not None, qa.chat_log.new()[-600:])

    # Dropping a request, and a villager dying with one, free the player's slot.
    qa.run_as_player(f'villagerai request assign {qa.v("qa.v1")} fetch', r'Request from')
    qa.run_as_player('villagerai request accept', r'Request accepted')
    m = qa.say('Never mind, I give up', r'Request dropped:')
    check('abandon: "never mind" drops an accepted request', m is not None and qa.request('qa.v1') is None,
          qa.chat_log.new()[-600:])
    qa.run_as_player(f'villagerai request assign {qa.v("qa.v3")} fetch', r'Request from')
    qa.run_as_player('villagerai request accept', r'Request accepted')
    m = qa.say('/villagerai requests', r'Village requests \(10 finished\)[\s\S]*Bring')
    check('requests: the open request is listed', m is not None, qa.chat_log.new()[-600:])
    qa.cmd(f'kill {qa.v("qa.v3")}')
    time.sleep(1)
    m = qa.say('/villagerai requests', r'None open')
    check('death: a dead villager\'s request no longer counts', m is not None, qa.chat_log.new()[-600:])
    mock('/qa/fail?on=0')

    # Villager data written by 1.x loads unchanged.
    out = qa.cmd(f'summon minecraft:villager -6.5 {Y} 0.5 {{NoAI:1b,Tags:["qa.old"],{NS}:{{Memory:{{rapport:3,players:{{}}}},'
                 f'PersonaOverride:{{name:"Oldtimer",trait:"stubborn"}}}}}}')
    time.sleep(1)
    rapport = qa.cmd(f'data get entity {qa.v("qa.old")} {NS}.Memory.rapport')
    persona = qa.cmd(f'data get entity {qa.v("qa.old")} {NS}.PersonaOverride.name')
    check('compat: 1.x villager data loads (rapport, persona)', re.search(r'\b3\b', rapport) is not None and 'Oldtimer' in persona,
          f'{out} / {rapport} / {persona}')

    # A bubble left on a villager by a crash is removed.
    qa.cmd(f'summon minecraft:villager -6.5 {Y} 4.5 {{NoAI:1b,Tags:["qa.orphan"],Passengers:[{{id:"minecraft:text_display",'
           f'alignment:"center",Tags:["smartvillagers.bubble","qa.orphanbubble"]}}]}}')
    gone = False
    for _ in range(10):
        time.sleep(1)
        if 'passed' not in qa.cmd('execute if entity @e[type=minecraft:text_display,tag=qa.orphanbubble]').lower():
            gone = True
            break
    check('bubble: orphaned bubble removed', gone)


def log_checks(log):
    text = Path(log).read_text(errors='replace')
    errors = [line for line in text.splitlines()
              if re.search(r'/(ERROR|FATAL)\]|Exception|Couldn\'t parse|Failed to (load|parse)|Unknown (key|element)', line)
              and 'Narrator' not in line and 'realms' not in line.lower()]
    mod_errors = [line for line in errors if NS in line.lower() or 'smart villagers' in line.lower()
                  or 'advancement' in line.lower() or 'mixin' in line.lower()]
    check('log: no errors about the mod, its mixins or advancements', not mod_errors, mod_errors[:5])
    check('log: no errors at all', not errors, errors[:5])
    check('log: mod ready', 'Smart Villagers AI ready' in text)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--port', type=int, default=25575)
    ap.add_argument('--log', help='server log to scan for errors afterwards')
    ap.add_argument('--client-log', required=True, help='client log (chat lines are read from it)')
    ap.add_argument('--world', help='server world folder (for the ledger)')
    ap.add_argument('--shots', help='where to save screenshots')
    args = ap.parse_args()
    mock('/qa/fail?on=0')
    qa = Qa(args)
    setup(qa)
    try:
        run(qa)
    finally:
        mock('/qa/fail?on=0')
    if args.log:
        log_checks(args.log)
    print(f'\n{len(FAILURES)} failure(s)' + (': ' + ', '.join(FAILURES) if FAILURES else ''), flush=True)
    return 1 if FAILURES else 0


if __name__ == '__main__':
    sys.exit(main())
