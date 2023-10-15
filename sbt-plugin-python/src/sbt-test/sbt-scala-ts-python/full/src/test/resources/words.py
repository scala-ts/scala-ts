# Generated by ScalaTS 0.5.18-SNAPSHOT: https://scala-ts.github.io/scala-ts/

from dataclasses import dataclass  # noqa: F401
import typing  # noqa: F401
import datetime  # noqa: F401
import time  # noqa: F401

from generated import greeting  # noqa: F401
from generated.greeting import Greeting


# Declare singleton Words
class WordsInvariantsFactory:
    @classmethod
    def start(self) -> typing.List[Greeting]:
        return [greeting.GreetingCompanion.Hello(), greeting.GreetingCompanion.Hi()]


@dataclass
class IWordsInvariants:
    start: typing.List[Greeting]


WordsInvariants = IWordsInvariants(
    start=WordsInvariantsFactory.start(),
)
