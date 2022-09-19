import unittest

from generated import foolure
from generated.foolure import FooLure


class FooLureSuite(unittest.TestCase):

    def test(self):
        v: FooLure = 'FooLure'
        self.assertEqual(v, foolure.FooLureInhabitant)
        
