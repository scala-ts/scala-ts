import unittest

from generated import ipsum
from generated.ipsum import Ipsum


class IpsumSuite(unittest.TestCase):

    def test(self):
        v: Ipsum = 'Ipsum'
        self.assertEqual(v, ipsum.IpsumInhabitant)
        
