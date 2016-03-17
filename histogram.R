args <- commandArgs(TRUE)
baseRunPath <- args[1]
runPath <- args[2]
outputTxt <- args[3]
outputPdf <- args[4]

library("ggplot2")
truth <- read.table(paste0(baseRunPath,"/ITERS/it.0/perso-dist-histo.txt",sep=""), header=TRUE)
truth <- within(truth, case<-"truth")
initial <- read.table(paste0(runPath,"/ITERS/it.0/perso-dist-histo.txt",sep=""), header=TRUE)
initial <- within(initial, case<-"initial")
calibrated <- read.table(paste0(runPath,"/ITERS/it.100/perso-dist-histo.txt",sep=""), header=TRUE)
calibrated <- within(calibrated, case<-"calibrated")
all <- rbind(truth, initial, calibrated)
all <- within(all, case <- factor(case, levels=(c("initial", "calibrated", "truth"))))
ggplot(subset(all, distance < 200000), aes(x=distance/1000, fill=case)) +
    geom_histogram(position="dodge", binwidth = 20) +
    theme_bw() +
    labs(x = "All-day travel distance [km]", y = "Number of Inviduals", fill = "Case") +
    scale_fill_manual(values=c("grey70", "grey50", "black")) +
    theme(legend.position="bottom")
write.table(all, file=outputTxt, row.names=FALSE)
ggsave(outputPdf)
