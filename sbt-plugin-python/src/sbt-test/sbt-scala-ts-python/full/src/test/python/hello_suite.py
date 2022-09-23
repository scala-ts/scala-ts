import unittest

from generated import hello
from generated.hello import Hello


class HelloSuite(unittest.TestCase):

    def test(self):
        v: Hello = 'Hello'
        self.assertEqual(v, hello.HelloInhabitant)
        
