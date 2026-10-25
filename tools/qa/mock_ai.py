#!/usr/bin/env python3
"""A stand-in OpenAI-compatible Chat Completions server for QA (no key, no network): the dev server's mod config
points at it (see devrun.sh prepare-server). Every request is appended to a JSON-lines log so checks can read the
prompt the villager sent, and the reply quotes the player's words so checks can find it in the client's chat.

  python3 tools/qa/mock_ai.py [--port 18080] [--log /tmp/sv-qa/ai-requests.jsonl]

Control (GET): /qa/fail?on=1 makes every completion fail with HTTP 500 (the mod then uses local dialogue),
/qa/fail?on=0 restores replies, /qa/delay?s=8 answers after 8 seconds (to capture the "thinking" bubble),
/qa/reset clears the log.
"""
from __future__ import annotations

import argparse
import json
import re
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

STATE = {'fail': False, 'delay': 0.0}


def reply_for(system: str, user: str) -> str:
    said = re.search(r'The player says: "(.*)"', user, re.S)
    said = (said.group(1) if said else user)[:40]
    name = re.search(r'You are ([^,]+),', system)
    who = name.group(1) if name else 'villager'
    if 'asking the player for a favour' in system:
        return f'QA-AI {who} asks a favour. (heard: "{said}")'
    if 'just finished your request' in system:
        return f'QA-AI {who} says thank you! (heard: "{said}")'
    return f'QA-AI {who} replies. (heard: "{said}")'


class Handler(BaseHTTPRequestHandler):
    log_path: Path = Path('/tmp/sv-qa/ai-requests.jsonl')

    def log_message(self, *args):
        pass

    def _send(self, code, body):
        data = json.dumps(body).encode()
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        url = urlparse(self.path)
        if url.path == '/qa/fail':
            STATE['fail'] = parse_qs(url.query).get('on', ['1'])[0] == '1'
        elif url.path == '/qa/delay':
            STATE['delay'] = float(parse_qs(url.query).get('s', ['0'])[0])
        elif url.path == '/qa/reset':
            self.log_path.write_text('')
        self._send(200, dict(STATE))

    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers.get('Content-Length', 0))) or b'{}')
        messages = body.get('messages', [])
        system = next((m['content'] for m in messages if m.get('role') == 'system'), '')
        user = next((m['content'] for m in messages if m.get('role') == 'user'), '')
        with self.log_path.open('a') as out:
            out.write(json.dumps({'path': self.path, 'model': body.get('model'), 'system': system, 'user': user,
                                  'failed': STATE['fail']}) + '\n')
        if STATE['delay']:
            time.sleep(STATE['delay'])
        if STATE['fail'] or not self.path.endswith('/chat/completions'):
            self._send(500, {'error': {'message': 'QA mock failure'}})
            return
        self._send(200, {'id': 'qa', 'object': 'chat.completion', 'model': body.get('model'),
                         'choices': [{'index': 0, 'finish_reason': 'stop',
                                      'message': {'role': 'assistant', 'content': reply_for(system, user)}}]})


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--port', type=int, default=18080)
    ap.add_argument('--log', default='/tmp/sv-qa/ai-requests.jsonl')
    args = ap.parse_args()
    Handler.log_path = Path(args.log)
    Handler.log_path.parent.mkdir(parents=True, exist_ok=True)
    Handler.log_path.touch()
    ThreadingHTTPServer(('127.0.0.1', args.port), Handler).serve_forever()


if __name__ == '__main__':
    main()
