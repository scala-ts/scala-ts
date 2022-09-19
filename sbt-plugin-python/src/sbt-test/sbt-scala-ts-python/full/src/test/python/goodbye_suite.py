import unittest

from generated import goodbye
from generated.goodbye import GoodBye


class GoodByeSuite(unittest.TestCase):

    def test(self):
        v: GoodBye = 'GoodBye'
        self.assertEqual(v, goodbye.GoodByeInhabitant)
        
