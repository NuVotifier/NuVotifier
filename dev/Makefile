
.PHONY: clean all run

all : resources/paper.jar resources/waterfall.jar resources/velocity.jar resources/sponge.jar resources/viaversion.jar

resources :
	mkdir resources

resources/paper.jar : resources
	wget -O resources/paper.jar https://papermc.io/api/v1/paper/1.16.2/182/download

resources/waterfall.jar : resources
	wget -O resources/waterfall.jar https://papermc.io/ci/job/Waterfall/lastSuccessfulBuild/artifact/Waterfall-Proxy/bootstrap/target/Waterfall.jar

resources/velocity.jar : resources
	wget -O resources/velocity.jar https://ci.velocitypowered.com/job/velocity-1.1.0/lastSuccessfulBuild/artifact/proxy/build/libs/velocity-proxy-1.1.0-SNAPSHOT-all.jar

resources/sponge.jar : resources
	wget -O resources/sponge.jar https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/1.12.2-7.3.0/spongevanilla-1.12.2-7.3.0.jar

resources/viaversion.jar : resources
	wget -O resources/viaversion.jar https://ci.viaversion.com/job/ViaVersion/459/artifact/jar/target/ViaVersion-3.1.1-SNAPSHOT.jar

clean :
	rm -r resources/

run :
	tmux new-session './p1/run.sh' \; \
	    split-window -h './p2/run.sh' \; \
		split-window -h './s1/run.sh' \; \
		split-window -h './s2/run.sh' \; \
		select-layout even-horizontal
