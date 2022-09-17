# Generated by ScalaTS 0.5.14-SNAPSHOT: https://scala-ts.github.io/scala-ts/

from dataclasses import dataclass  # noqa: F401
import typing  # noqa: F401
import datetime  # noqa: F401


# Declare singleton Alabama
Alabama = typing.Literal["AL"]
AlabamaInhabitant: Alabama = "AL"


class AlabamaInvariantsFactory:
    @classmethod
    def entryName(self) -> str:
        return "AL"


@dataclass
class IAlabamaInvariants:
    entryName: str


AlabamaInvariants = IAlabamaInvariants(
    entryName=AlabamaInvariantsFactory.entryName(),
)
