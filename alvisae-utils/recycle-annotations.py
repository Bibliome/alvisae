#!/bin/env python

from subprocess import check_call
from os import mkdir, environ
from optparse import OptionParser
from sys import stderr
from tempfile import mkdtemp


class RecycleAnnotations(OptionParser):
    def __init__(self):
        OptionParser.__init__(self, usage='usage: %prog [options] CONFIG')
        self.add_option('--working-directory', action='store', type='string', dest='WD', help='directory where temporary and log files will be stored (/tmp/something if omitted)')
        self.add_option('--alvisnlp', action='store', type='string', dest='ALVISNLP', help='path to the AlvisNLP/ML executable')
        self.add_option('--psql-host', action='store', type='string', dest='PSQL_HOST', help='host name of the PostrgreSQL server')
        self.add_option('--psql-port', action='store', type='string', dest='PSQL_PORT', help='port of the PostrgreSQL server')
        self.add_option('--psql-db', action='store', type='string', dest='PSQL_DB', help='name of the AlvisAE database')
        self.add_option('--psql-schema', action='store', type='string', dest='PSQL_SCHEMA', help='name of the AlvisAE database schema')
        self.add_option('--psql-user', action='store', type='string', dest='PSQL_USER', help='PostrgreSQL user name')
        self.add_option('--psql-password', action='store', type='string', dest='PSQL_PASSWORD', help='PostrgreSQL user password')
        self.add_option('--source-campaign', action='store', type='string', dest='SOURCE_CAMPAIGN', help='identifier of the campaign where to read annotations')
        self.add_option('--source-task', action='store', type='string', dest='SOURCE_TASK', help='name of the task to read')
        self.add_option('--source-user', action='store', type='string', dest='SOURCE_USER', help='name of the user that owns the annotations to read')
        self.add_option('--source-documents', action='store', type='string', dest='SOURCE_DOCUMENTS', help='comma sparated list of document identifiers (internal ids, all documents if omitted)')
        self.add_option('--target-campaign', action='store', type='string', dest='TARGET_CAMPAIGN', help='identifier of the campaign where to write annotations (same as --source-campaign if omitted)')
        self.add_option('--target-users', action='store', type='string', dest='TARGET_USERS', help='name of the users that own the written annotations')
        self.add_option('--target-task', action='store', type='string', dest='TARGET_TASK', help='name of the task of the written annotations (same as --source-task if omitted)')
        self.add_option('--adjudicate', action='store_true', dest='ADJUDICATE', default=None, help='perform auto-adjudication for reviewing tasks')
        self.add_option('--java-home', action='store', type='string', dest='JAVA_HOME', help='path to java home directory')
        self.add_option('--alvisae-jar', action='store', type='string', dest='ALVISAE_JAR', help='path to the AlvisAE jar file')
        self.add_option('--annotation-types', action='store', type='string', dest='ANNOTATION_TYPES', help='comma separated list of annotation types to recycle (all annotations if omitted)')
        self.add_option('--keep-zones', action='store', type='string', dest='KEEP_ZONES', help='text annotation type of keep zones, annotations outside thes zones will be deleted')
        self.add_option('--keep-all-if-no-zone', action='store_true', dest='KEEP_ALL_IF_NO_ZONE', default=None, help='keep all document if there is no keep zone')
        self.add_option('--dry-run', action='store_true', dest='DRY_RUN', default=None, help='do not execute commands, just generate files')
        self.add_option('--import-plan', action='store', type='string', dest='IMPORT_PLAN', default=None, help='import the specified custom plan before re-injecting annotations')
        self.add_option('--alias', action='append', nargs=2, type='string', dest='ALIAS', default=[], help='alias value to pass to custom plan')
        self.add_option('--publish', action='store_true', dest='PUBLISH', default=None, help='publish the annotation (broken: the AlvisAE importer does not take it into account)')
        self.options = {}

    def _load_options_file(self, fn):
        with open(fn) as f:
            for line in f:
                k, q, v = line.partition('=')
                if q != '=':
                    raise Exception('missing \'=\'')
                self.options[k.strip()] = v.strip()

    def _load_options(self, args=None):
        if args is None:
            (options0, args0) = self.parse_args()
        else:
            (options0, args0) = self.parse_args(args)
        for fn in args0:
            self._load_options_file(fn)
        for k, v in options0.__dict__.items():
            if v is not None:
                self.options[k] = v

    def _check_options(self, *keys):
        for k in keys:
            if k not in self.options or self.options[k] is None:
                raise Exception('missing option ' + k)

    def _bool(self, key, stringize=False):
        if key in self.options:
            if self.options[key] is None:
                self.options[key] = False
            elif self.options[key] is True:
                pass
            elif self.options[key] is False:
                pass
            elif self.options[key] == '0' or self.options[key].lower() == 'false' or self.options[key].lower() == 'no':
                self.options[key] = False
            elif self.options[key] == '1' or self.options[key].lower() == 'true' or self.options[key].lower() == 'yes':
                self.options[key] = True
            else:
                raise Exception('could not understand ' + key + ' = ' + self.options[key])
        else:
            self.options[key] = False
        if stringize:
            self.options[key] = str(self.options[key]).lower()

    def _validate_options(self):
        self._check_options(
            'ALVISNLP',
            'PSQL_HOST',
            'PSQL_PORT',
            'PSQL_DB',
            'PSQL_SCHEMA',
            'PSQL_USER',
            'PSQL_PASSWORD',
            'SOURCE_CAMPAIGN',
            'SOURCE_TASK',
            'SOURCE_USER',
            'TARGET_USERS',
            'JAVA_HOME',
            'ALVISAE_JAR'
        )
        self._bool('DRY_RUN')
        if 'WD' not in self.options:
            self.options['WD'] = mkdtemp('', 'recycle-annotations__', '/tmp')
        if 'SOURCE_DOCUMENTS' in self.options and self.options['SOURCE_DOCUMENTS']:
            self.options['DOC_IDS'] = '<docIds>%s</docIds>' % self.options['SOURCE_DOCUMENTS']
        else:
            self.options['DOC_IDS'] = ''
        self.options['TARGET_USERS_LIST'] = tuple(u.strip() for u in self.options['TARGET_USERS'].split(','))
        if 'ANNOTATION_TYPES' in self.options and self.options['ANNOTATION_TYPES']:
            self.options['TYPE_FILTER'] = ' or '.join(('@name == "%s"' % t) for t in self.options['ANNOTATION_TYPES'].split(','))
        else:
            self.options['TYPE_FILTER'] = 'true'
        if 'TARGET_TASK' not in self.options or not self.options['TARGET_TASK']:
            self.options['TARGET_TASK'] = self.options['SOURCE_TASK']
        if 'TARGET_CAMPAIGN' not in self.options or not self.options['TARGET_CAMPAIGN']:
            self.options['TARGET_CAMPAIGN'] = self.options['SOURCE_CAMPAIGN']
        if 'KEEP_ZONES' not in self.options:
            self.options['KEEP_ZONES'] = 'dummy'
            self.options['KEEP_ALL_IF_NO_ZONE'] = True
        self._bool('KEEP_ALL_IF_NO_ZONE', True)
        self._bool('ADJUDICATE')
        if self.options['ADJUDICATE']:
            self.options['ADJUDICATE_PARAM'] = '<loadDependencies/> <adjudicate/>'
        else:
            self.options['ADJUDICATE_PARAM'] = ''
        self._bool('PUBLISH', True)
        if 'IMPORT_PLAN' in self.options and self.options['IMPORT_PLAN']:
            if 'ALIAS' in self.options and self.options['ALIAS']:
                self.options['IMPORT_PLAN'] = '<imported-plan href="%s">\n%s\n  </imported-plan>' % (self.options['IMPORT_PLAN'], '\n'.join('    <%s>%s</%s>' % (k, v, k) for k, v in self.options['ALIAS']))
            else:
                self.options['IMPORT_PLAN'] = '<imported-plan href="%s"/>' % self.options['IMPORT_PLAN']
        else:
            self.options['IMPORT_PLAN'] = ''
        self.options['LOG'] = self.options['WD'] + '/import.log'
        self.options['PLAN'] = self.options['WD'] + '/import.plan'
        self.options['JSON_DIR'] = self.options['WD'] + '/json'
        self.options['PROPS_FILE'] = self.options['WD'] + '/db.props'
        self.options['ALVISNLP_CL_SAVE'] = self.options['WD'] + '/alvisnlp.sh'
        self.options['ALVISAE_CL_SAVE'] = self.options['WD'] + '/alvisae.sh'
        self.options['USER_FILE'] = self.options['WD'] + '/users.csv'
        self.options['TASK_FILE'] = self.options['WD'] + '/tasks.csv'
        self.options['FEATURE_FILTER'] = '@key != "referent" and @key != "id" and @key != "__TYPE" and @key != "created" and @key != "annotation-set" and @key != "user" and @key != "unmatched"'

    def run(self):
        self._load_options()
        self._validate_options()
        self.recycle()

    def _call(self, cl, force=False):
        if self.options['DRY_RUN'] and not force:
            stderr.write('Skipping: %s\n' % cl)
            return None
        env = dict(environ)
        env['JAVA_HOME'] = self.options['JAVA_HOME']
        env['PATH'] = self.options['JAVA_HOME'] + '/bin:' + env['PATH']
        return check_call(cl.split(' '), env=env)

    def recycle(self):
        stderr.write('Working directory: %s\n' % self.options['WD'])
        self._write_file(self.options['PROPS_FILE'], PROPERTIES)
        self._write_file(self.options['PLAN'], SOURCE_PLAN)
        self._write_file(self.options['ALVISNLP_CL_SAVE'], SOURCE_CL)
        try:
            mkdir(self.options['JSON_DIR'])
        except OSError:
            pass
        self._call(SOURCE_CL % self.options, True)

        self._write_file(self.options['ALVISAE_CL_SAVE'], TARGET_CL)
        cli = TARGET_CL % self.options
        for u in self.options['TARGET_USERS_LIST']:
            self.options['TARGET_USER'] = u
            self._write_file(self.options['USER_FILE'], '0\t%(TARGET_USER)s\n')
            self._write_file(self.options['TASK_FILE'], '0\t%(TARGET_TASK)s\n')
            self._call(cli)

    def _write_file(self, path, tpl):
        f = open(path, 'w')
        f.write(tpl % self.options)
        f.close()


TARGET_CL = '''%(JAVA_HOME)s/bin/java -jar %(ALVISAE_JAR)s --import-annotations -p %(PROPS_FILE)s -d %(JSON_DIR)s --campaignId %(TARGET_CAMPAIGN)s --userList %(USER_FILE)s --taskList %(TASK_FILE)s'''


PROPERTIES = '''db.type=postgresql
db.server=%(PSQL_HOST)s
db.port=%(PSQL_PORT)s
db.dbname=%(PSQL_DB)s
db.username=%(PSQL_USER)s
db.password=%(PSQL_PASSWORD)s
db.schema=%(PSQL_SCHEMA)s
'''


SOURCE_CL = '''%(ALVISNLP)s -log %(LOG)s -verbose %(PLAN)s'''

SOURCE_PLAN = '''
<alvisnlp-plan id="export-manual">
  <read class="AlvisAEReader">
    <databasePropsFile>%(PROPS_FILE)s</databasePropsFile>
    <campaignId>%(SOURCE_CAMPAIGN)s</campaignId>
    <taskName>%(SOURCE_TASK)s</taskName>
    <userFeature>user</userFeature>
    <userNames>%(SOURCE_USER)s</userNames>
    %(DOC_IDS)s
    <section>text</section>
    <fragmentsLayer>user</fragmentsLayer>
    <typeFeature>__TYPE</typeFeature>
    <fragmentTypeFeature>__TYPE</fragmentTypeFeature>
    %(ADJUDICATE_PARAM)s
  </read>

  <keep-zones>
    <active>"%(KEEP_ZONES)s" != ""</active>

    <keep-all-if-no-zone>
      <active>%(KEEP_ALL_IF_NO_ZONE)s</active>

      <create-rel class="Action">
        <target>documents.sections</target>
        <action>new:relation("%(KEEP_ZONES)s")</action>
        <createRelations/>
      </create-rel>

      <create-zones class="Action">
        <target>documents[not sections.relations[@kind == "text-bound" and @name == "%(KEEP_ZONES)s"].tuples].sections</target>
        <action>
          relations:%(KEEP_ZONES)s.new:tuple.(
            set:feat:__TYPE("%(KEEP_ZONES)s")
          | set:feat:user("dummy")
          | set:arg:frag0(target.new:annotation:user(0, str:len(target.contents)).(set:feat:__TYPE("%(KEEP_ZONES)s")))
          )
        </action>
        <createTuples/>
        <setArguments/>
        <setFeatures/>
        <createAnnotations/>
        <addToLayer/>
      </create-zones>
    </keep-all-if-no-zone>

    <text-annotations class="Action">
      <target>documents.sections.relations[@kind == "text-bound" and @name != "%(KEEP_ZONES)s"].tuples[args[not outside:user[@__TYPE == "%(KEEP_ZONES)s"]]]</target>
      <action>$.set:feat:delete("delete")</action>
      <setFeatures/>
    </text-annotations>

    <compound-annotations-1 class="Action">
      <target>documents.sections.relations.tuples[@delete != "delete" and args[@delete == "delete"]]</target>
      <action>set:feat:delete("delete")</action>
      <setFeatures/>
    </compound-annotations-1>

    <compound-annotations-2 class="Action">
      <target>documents.sections.relations.tuples[@delete != "delete" and args[@delete == "delete"]]</target>
      <action>set:feat:delete("delete")</action>
      <setFeatures/>
    </compound-annotations-2>

    <compound-annotations-3 class="Action">
      <target>documents.sections.relations.tuples[@delete != "delete" and args[@delete == "delete"]]</target>
      <action>set:feat:delete("delete")</action>
      <setFeatures/>
    </compound-annotations-3>

    <compound-annotations-4 class="Action">
      <target>documents.sections.relations.tuples[@delete != "delete" and args[@delete == "delete"]]</target>
      <action>set:feat:delete("delete")</action>
      <setFeatures/>
    </compound-annotations-4>

    <delete-them class="Action">
      <target>documents.sections.relations.tuples[@delete == "delete"]</target>
      <action>delete</action>
      <deleteElements/>
    </delete-them>

    <delete-keep-zones class="Action">
      <target>documents.sections.relations:'%(KEEP_ZONES)s'.tuples</target>
      <action>delete</action>
      <deleteElements/>
    </delete-keep-zones>
  </keep-zones>

  %(IMPORT_PLAN)s

  <json class="AlvisAEWriter">
    <outDir>%(JSON_DIR)s</outDir>
    <documentDescription>@description</documentDescription>
    <documentProperties>DocumentID=@external-id</documentProperties>
    <publish>%(PUBLISH)s</publish>
    <annotationSets>
      <element type="UserAnnotation" description="imported from %(SOURCE_TASK)s by %(SOURCE_USER)s in campaign %(SOURCE_CAMPAIGN)s">
        <text>
          <instances>relations[(%(TYPE_FILTER)s) and @kind == "text-bound"].tuples[@referent == "true"]</instances>
          <type>relation.@name</type>
          <fragments>nav:arguments[@role ^= "frag"]</fragments>
          <propdef>nav:features[%(FEATURE_FILTER)s]</propdef>
        </text>

        <group>
          <instances>relations[(%(TYPE_FILTER)s) and @kind == "group"].tuples[@referent == "true"]</instances>
          <type>relation.@name</type>
          <items>nav:arguments[@role ^= "item"]</items>
          <propdef>nav:features[%(FEATURE_FILTER)s]</propdef>
        </group>

        <relation>
          <instances>relations[(%(TYPE_FILTER)s) and @kind == "relation"].tuples[@referent == "true"]</instances>
          <type>relation.@name</type>
          <argdef>nav:arguments[not @role ^= "source"]</argdef>
          <role>@role</role>
          <propdef>nav:features[%(FEATURE_FILTER)s]</propdef>
        </relation>
      </element>
    </annotationSets>
  </json>
</alvisnlp-plan>
'''

if __name__ == '__main__':
    RecycleAnnotations().run()
