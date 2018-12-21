#!/bin/sh
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd $DIR
echo <<WARNING
Hey! Just so you know, I am using the IReallyKnowWhatIAmDoingISwear flag, as
well as overriding the eula check. If you don't agree to the Mojang EULA
thingie, then uh, smash your computer or something because you totally agreed
to it so just fyi
WARNING
java -DIReallyKnowWhatIAmDoingISwear -Dcom.mojang.eula.agree=true -jar paper.jar
