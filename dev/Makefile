
.PHONY: clean, all, run

all : resources/paper.jar resources/proxy.jar

resources :
	mkdir resources

resources/paper.jar : resources
	wget -O resources/paper.jar https://papermc.io/ci/job/Paper-1.13/lastSuccessfulBuild/artifact/paperclip.jar

resources/proxy.jar : resources
	wget -O resources/proxy.jar https://papermc.io/ci/job/Waterfall/lastSuccessfulBuild/artifact/Waterfall-Proxy/bootstrap/target/Waterfall.jar

clean :
	rm -r resources/

run :
	tmux new-session './proxy/run.sh' \; \
		split-window -h './s1/run.sh' \; \
		split-window -h './s2/run.sh' \; \
		select-layout even-horizontal
