library("ggplot2")

args <- commandArgs(TRUE)
baseRunPath <- args[1]
runPath <- args[2]
outputPath <- args[3]
outputPathPng <- args[4]

histo0nonworker <- read.table(paste0(runPath,"/ITERS/it.0/legHistogram-nonworkers.txt",sep=""), header=TRUE)
histo100nonworker <- read.table(paste0(runPath,"/ITERS/it.100/legHistogram-nonworkers.txt",sep=""), header=TRUE)
histobasenonworker <- read.table(paste0(baseRunPath, "/ITERS/it.0/legHistogram-nonworkers.txt",sep=""), header=TRUE)

histo0nonworker["group"] = "initial"
histo100nonworker["group"] = "calibrated"
histobasenonworker["group"] = "truth"
histo0nonworker["stratum"] = "nonworkers"
histo100nonworker["stratum"] = "nonworkers"
histobasenonworker["stratum"] = "nonworkers"

histo0worker <- read.table(paste0(runPath,"/ITERS/it.0/legHistogram-workers.txt",sep=""), header=TRUE)
histo100worker <- read.table(paste0(runPath,"/ITERS/it.100/legHistogram-workers.txt",sep=""), header=TRUE)
histobaseworker <- read.table(paste0(baseRunPath, "/ITERS/it.0/legHistogram-workers.txt",sep=""), header=TRUE)

histo0worker["group"] = "initial"
histo100worker["group"] = "calibrated"
histobaseworker["group"] = "truth"
histo0worker["stratum"] = "workers"
histo100worker["stratum"] = "workers"
histobaseworker["stratum"] = "workers"

columns <- c("time", "time.1", "en.route_car", "group", "stratum")

jointhisto <- rbind(histo0nonworker[,columns], histo100nonworker[,columns], histobasenonworker[,columns], histo0worker[,columns], histo100worker[,columns], histobaseworker[,columns])

jointhisto <- within(jointhisto, group <- factor(group, levels=c("initial", "calibrated", "truth")))
jointhisto <- within(jointhisto, stratum <- factor(stratum, levels=c("workers", "nonworkers")))

ggplot(subset(jointhisto, time.1%%2400 == 0 & time.1 < (29*60*60)), aes(x=time.1/(60*60), y=en.route_car, group=group)) + geom_line(aes(linetype=group)) + xlab("Time of day [h]") + ylab("Number of cars on network") + labs(linetype='Case') + scale_x_continuous(breaks=seq(0,25,6))+ scale_linetype_manual(values=c("dotted", "dotdash", "solid")) + facet_grid(.~stratum) + theme_bw()

ggsave(outputPath,height=3.5, width=7)
ggsave(outputPathPng,height=3.5, width=7)
