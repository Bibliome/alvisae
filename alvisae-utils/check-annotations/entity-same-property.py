import alvisnlp
import sys
import collections


CORPUS = alvisnlp.Corpus.parse_json(sys.stdin)
PROPERTY = CORPUS.params['property']
ci = CORPUS.params['caseInsensitive']
CI = ci == 'yes' or ci == 'y' or ci == 'true'
DICT = collections.defaultdict(lambda: collections.defaultdict(list))
for doc in CORPUS.documents:
    for sec in doc.sections:
        for rel in sec.relations:
            for t in rel.tuples:
                form = t.features['form']
                if CI:
                    form = form.lower()
                value = ','.join(sorted(t.features.get(PROPERTY)))
                DICT[form][value].append(t)


def tuple_str(t):
    doc = t.relation.section.document
    return '[c=%s (%s), d=%s (%s), o=%s-%s]' % (doc.features['campaign-id'], doc.features['campaign-name'], doc.identifier, doc.features['external-id'], t.features['start_'], t.features['end_'])


sys.stdout.write('FORM\tPROPERTY (%s)\tN_OCC\tOCCURRENCES\n' % PROPERTY)
for form, values in DICT.items():
    if len(values) >= 2:
        for value, tuples in values.items():
            sys.stdout.write('\t'.join([form, value, str(len(tuples)), '; '.join(tuple_str(t) for t in tuples)]))
            sys.stdout.write('\n')
        form = ''
        values = ''
