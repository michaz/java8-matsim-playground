library("ggplot2")
perso <- read.table("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested3/alternatives/trajectoryenrichment100.0/perso-dist-histo.txt", header=TRUE)
dummyperso <- read.table("/Users/michaelzilske/shared-svn/projects/mzilske/cdr-with-cadyts/dummydata.txt", header=TRUE)
nperso <- rbind(perso, dummyperso)
nperso <- within(nperso, rate <- factor(rate, levels=(c(0,5))))
nperso <- within(nperso, Case <- factor(Case, levels=(c("initial", "calibrated", "truth"))))

ggplot(subset(nperso, kilometers < 200000 & (rate==0 | rate==5) & (Case=="truth" | Case =="calibrated" | Case=="initial") & status=="all"), aes(x=kilometers/1000, fill=Case)) + geom_bar(position="dodge", binwidth=12.5) + xlab("All-day travel distance [km]") + ylab("Number of Individuals") + facet_grid(rate ~ status) + theme_bw() + opts(legend.position="bottom") + scale_fill_manual(values=c("grey70", "grey50", "black"))

ggsave("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/regimes/uncongested3/alternatives/trajectoryenrichment100.0/worker-nonworker-histo-2.pdf")
