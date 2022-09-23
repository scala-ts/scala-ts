import unittest

from generated.classwithtypeargs import ClassWithTypeArgs
from generated.classwithgenericfield import ClassWithGenericField


class ClassWithGenericFieldSuite(unittest.TestCase):

    def test(self):
        arg: ClassWithTypeArgs[float] = ClassWithTypeArgs[float](
            name='arg',
            value=1.23)

        v: ClassWithGenericField = ClassWithGenericField(field=arg)

        self.assertEqual(v.field.name, 'arg')
        self.assertEqual(v.field.value, 1.23)
        
