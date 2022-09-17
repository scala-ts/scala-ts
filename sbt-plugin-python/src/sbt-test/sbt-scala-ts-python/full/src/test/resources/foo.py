# Generated by ScalaTS 0.5.14-SNAPSHOT: https://scala-ts.github.io/scala-ts/

from dataclasses import dataclass
import typing  # noqa: F401
import datetime  # noqa: F401

import transport  # noqa: F401
from transport import Transport


# Declare interface Foo
@dataclass
class Foo:
    id: int
    namesp: typing.Tuple[int, str]
    row: typing.Tuple[str, Transport, datetime.datetime]
    score: typing.Union[int, str]
    rates: typing.Dict[str, float]
