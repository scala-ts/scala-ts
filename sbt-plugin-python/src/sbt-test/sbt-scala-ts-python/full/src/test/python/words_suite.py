import unittest

import typing

from generated.words import WordsInvariants
from generated.greeting import GreetingCompanion


class WordsSuite(unittest.TestCase):

    def test(self):
        self.assertEqual(WordsInvariants.start, ['Hello', 'Hi'])
        
        
