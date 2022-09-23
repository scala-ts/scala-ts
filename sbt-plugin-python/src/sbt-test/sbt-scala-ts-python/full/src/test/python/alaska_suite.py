import unittest

from generated import alaska
from generated.alaska import Alaska

class AlaskaSuite(unittest.TestCase):

    def test(self):
        al: Alaska = 'AK'
        self.assertEqual(al, alaska.AlaskaInvariants.entryName)
        self.assertEqual(al, alaska.AlaskaInhabitant)
        
