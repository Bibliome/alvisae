#!/bin/env python

import argparse
import os
import os.path
import subprocess
import sys


class AlvisAEApp(argparse.ArgumentParser):
    def __init__(self, description, action):
        argparse.ArgumentParser.__init__(self, description=description)
        self.action = action
        self.add_argument('--java-home', metavar='PATH', dest='java_home', default=os.environ['JAVA_HOME'], help='Path to JDK home')
        self.add_argument('--alvisae-jar', metavar='JARFILE', dest='alvisae_jar', default='./AlvisAE-cli-modified.jar', help='Path to AlvisAE CLI JAR file')
        self.add_argument('--db-props', metavar='DBPROPS', dest='db_props', default='./db.props', help='Path to annotation database properties')

    def _begin_cli(self, args):
        return [
            os.path.join(args.java_home, 'bin', 'java'),
            '-jar',
            args.alvisae_jar,
            self.action,
            '-p',
            args.db_props
        ]

    def launch(self, args, end_cli):
        cli = self._begin_cli(args) + end_cli
        result = subprocess.run(cli, capture_output=True, encoding='utf8')
        self.handle_stdout(result.stdout)
        self.handle_stderr(result.stderr)

    def handle_stdout(self, stdout):
        sys.stdout.write(stdout)

    def handle_stderr(self, stderr):
        sys.stderr.write(stderr)
