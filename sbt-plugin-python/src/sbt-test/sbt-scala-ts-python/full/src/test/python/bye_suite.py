import unittest

from generated import bye
from generated.bye import Bye


class ByeSuite(unittest.TestCase):

    def test(self):
        v: Bye = 'Bye'
        self.assertEqual(v, bye.ByeInhabitant)
        
