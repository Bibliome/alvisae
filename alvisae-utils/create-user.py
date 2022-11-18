#!/bin/env python

import alvisae


class CreateUser(alvisae.AlvisAEApp):
    def __init__(self):
        alvisae.AlvisAEApp.__init__(self, 'creates a user', '--create-user')
        self.add_argument('--login', metavar='NAME', dest='login', default=None, help='User name')
        self.add_argument('--password', metavar='PASSWORD', dest='password', required=True, help='User password')
        self.add_argument('--multiple', metavar='FILE', dest='multiple', default=None, help='Create multiple users with the same password, logins in specified file')

    def run(self):
        args = self.parse_args()
        if args.multiple is None:
            if args.login is None:
                raise RuntimeError('either --login or --multiple is required')
            end_cli = [
                '--userName',
                args.login,
                '--password',
                args.password
            ]
            self.launch(args, end_cli)
        else:
            if args.login is not None:
                raise RuntimeError('--login or --multiple are mutually exclusive')
            with open(args.multiple) as f:
                for line in f:
                    login = line.strip()
                    if login == '':
                        continue
                    end_cli = [
                        '--userName',
                        login,
                        '--password',
                        args.password
                    ]
                    self.launch(args, end_cli)


if __name__ == '__main__':
    CreateUser().run()
