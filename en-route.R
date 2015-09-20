library("ggplot2")

args <- commandArgs(TRUE)
baseRunPath <- args[1]
runPath <- args[2]
outputPath <- args[3]

histo0 <- read.table(paste0(runPath,"/ITERS/it.0/0.legHistogram.txt",sep=""), header=TRUE)
histo100 <- read.table(paste0(runPath,"/ITERS/it.100/100.legHistogram.txt",sep=""), header=TRUE)
histobase <- read.table(paste0(baseRunPath, "/ITERS/it.0/0.legHistogram.txt",sep=""), header=TRUE)

histo0["group"] = "initial"
histo100["group"] = "calibrated"
histobase["group"] = "truth"


columns <- c("time", "time.1", "en.route_car", "group")


jointhisto <- rbind(histo0[,columns], histo100[,columns], histobase[,columns])

jointhisto <- within(jointhisto, group <- factor(group, levels=c("initial", "calibrated", "truth")))
ggplot(subset(jointhisto, time.1%%2400 == 0 & time.1 < (29*60*60)), aes(x=time.1/(60*60), y=en.route_car, group=group)) + geom_line(aes(linetype=group)) + xlab("Time of day [h]") + ylab("Number of cars on network") + labs(linetype='Case') + scale_x_continuous(breaks=seq(0,25,1))+ scale_linetype_manual(values=c("dotted", "dotdash", "solid")) + theme_bw() + opts(legend.position="bottom")

ggsave(outputPath,height=3.5, width=7)

# ggplot(subset(jointhisto, time.1%%2400 == 0 & time.1 < (29*60*60) & group2!="all" & rate==5), aes(x=time.1/(60*60), y=en.route_car, group=group)) + geom_line(aes(linetype=group)) + xlab("Time of day [h]") + ylab("Number of cars on network") + labs(linetype='Case') + scale_x_continuous(breaks=seq(0,25,6))+ scale_linetype_manual(values=c("dotted", "dotdash", "solid"))+ theme_bw() + facet_grid(.~group2) + opts(legend.position="bottom")

# ggsave(paste0(runPathRoot,"/en-route-worker-nonworker.pdf",sep=""), height=2, width=6)
