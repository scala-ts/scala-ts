import unittest

from generated.classwithtypeargs import ClassWithTypeArgs


class ClassWithTypeArgsSuite(unittest.TestCase):

    def test(self):
        v1: ClassWithTypeArgs[int] = ClassWithTypeArgs[int](
            name='withInt',
            value=1)

        self.assertEqual(v1.name, 'withInt')
        self.assertEqual(v1.value, 1)

        v2: ClassWithTypeArgs[float] = ClassWithTypeArgs[float](
            name='withFloat',
            value=2.3)

        self.assertEqual(v2.name, 'withFloat')
        self.assertEqual(v2.value, 2.3)
        
