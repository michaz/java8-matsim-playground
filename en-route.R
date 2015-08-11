library("ggplot2")

regimeRoot <- "/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested3"
runPathRoot <- paste0(regimeRoot, "/alternatives/trajectoryenrichment100.0randomlatitude", sep="")
runPath <- paste0(runPathRoot, "/rates/", sep="")

ratetable <- function(rate) {
histo52 <- read.table(paste0(runPath,rate,".0/1/ITERS/it.0/0.legHistogram.txt",sep=""), header=TRUE)
histo52workers <- read.table(paste0(runPath,rate,".0/1/ITERS/it.0/leghistogram-workers.txt",sep=""), header=TRUE)
histo52nonworkers <- read.table(paste0(runPath,rate,".0/1/ITERS/it.0/leghistogram-nonworkers.txt",sep=""), header=TRUE)
histo5100 <- read.table(paste0(runPath,rate,".0/1/ITERS/it.100/100.legHistogram.txt",sep=""), header=TRUE)
histo5100workers <- read.table(paste0(runPath,rate,".0/1/ITERS/it.100/leghistogram-workers.txt",sep=""), header=TRUE)
histo5100nonworkers <- read.table(paste0(runPath,rate,".0/1/ITERS/it.100/leghistogram-nonworkers.txt",sep=""), header=TRUE)

basePath <- paste0(regimeRoot, "/output-berlin/ITERS/it.0/", sep="")
histo5base <- read.table(paste0(basePath, "2kW.15.0.legHistogram.txt",sep=""), header=TRUE)
histo5baseworkers <- read.table(paste0(basePath, "leghistogram-workers.txt",sep=""), header=TRUE)
histo5basenonworkers <- read.table(paste0(basePath, "leghistogram-nonworkers.txt",sep=""), header=TRUE)

histo52["group"] = "initial"
histo52["group2"] = "all"
histo52workers["group"] = "initial"
histo52workers["group2"] = "workers"
histo52nonworkers["group"] = "initial"
histo52nonworkers["group2"] = "nonworkers"

histo5100["group"] = "calibrated"
histo5100["group2"] = "all"
histo5100workers["group"] = "calibrated"
histo5100workers["group2"] = "workers"
histo5100nonworkers["group"] = "calibrated"
histo5100nonworkers["group2"] = "nonworkers"

histo5base["group"] = "truth"
histo5base["group2"] = "all"
histo5baseworkers["group"] = "truth"
histo5baseworkers["group2"] = "workers"
histo5basenonworkers["group"] = "truth"
histo5basenonworkers["group2"] = "nonworkers"

columns <- c("time", "time.1", "en.route_car", "group", "group2")
cat(ncol(histo52),ncol(histo52workers),ncol(histo52nonworkers),ncol(histo5100),ncol(histo5100workers),ncol(histo5100nonworkers),ncol(histo5base),ncol(histo5baseworkers),ncol(histo5basenonworkers))

scaleFactor <- 1
histo52[,"en.route_car"] <- histo52[,"en.route_car"] * scaleFactor
histo5100[,"en.route_car"] <- histo5100[,"en.route_car"] * scaleFactor
histo52workers[,"en.route_car"] <- histo52workers[,"en.route_car"] * scaleFactor
histo52nonworkers[,"en.route_car"] <- histo52nonworkers[,"en.route_car"] * scaleFactor
histo5100workers[,"en.route_car"] <- histo5100workers[,"en.route_car"] * scaleFactor
histo5100nonworkers[,"en.route_car"] <- histo5100nonworkers[,"en.route_car"] * scaleFactor

histo5 <- rbind(histo52[,columns], histo52workers[,columns], histo52nonworkers[,columns], histo5100[,columns], histo5100workers[,columns], histo5100nonworkers[,columns], histo5base[,columns], histo5baseworkers[,columns], histo5basenonworkers[,columns])
histo5["rate"] = rate
return(histo5)
}

histo5 <- ratetable(5.0)
# histo0 <- ratetable(0)


# jointhisto <- rbind(histo0, histo5)
jointhisto <- histo5

jointhisto <- within(jointhisto, rate <- factor(rate, levels=c(0,5)))
jointhisto <- within(jointhisto, group2 <- factor(group2, levels=c("workers", "nonworkers", "all")))
jointhisto <- within(jointhisto, group <- factor(group, levels=c("initial", "calibrated", "truth")))
ggplot(subset(jointhisto, time.1%%2400 == 0 & time.1 < (29*60*60) & group2=="all"), aes(x=time.1/(60*60), y=en.route_car, group=group)) + geom_line(aes(linetype=group)) + xlab("Time of day [h]") + ylab("Number of cars on network") + labs(linetype='Case') + scale_x_continuous(breaks=seq(0,25,1))+ scale_linetype_manual(values=c("dotted", "dotdash", "solid")) + theme_bw() + facet_grid(rate~.) + opts(legend.position="bottom")

ggsave(paste0(runPathRoot,"/en-route.pdf",sep=""),height=3.5, width=7)

ggplot(subset(jointhisto, time.1%%2400 == 0 & time.1 < (29*60*60) & group2!="all" & rate==5), aes(x=time.1/(60*60), y=en.route_car, group=group)) + geom_line(aes(linetype=group)) + xlab("Time of day [h]") + ylab("Number of cars on network") + labs(linetype='Case') + scale_x_continuous(breaks=seq(0,25,6))+ scale_linetype_manual(values=c("dotted", "dotdash", "solid"))+ theme_bw() + facet_grid(.~group2) + opts(legend.position="bottom")

ggsave(paste0(runPathRoot,"/en-route-worker-nonworker.pdf",sep=""), height=2, width=6)
