library("ggplot2")
linkstats8 <- read.table("output/illustrative/8-count/linkstats.txt", header=TRUE)
linkstats18 <- read.table("output/illustrative/18-count/linkstats.txt", header=TRUE)
linkstats18["measurement.at"] = 18
linkstats8["measurement.at"] = 8
linkstats <- rbind(linkstats8, linkstats18)
annotation <- data.frame(hour=c(8,10,18,8,10,18,8,10,18,8,10,18), measurement.at=c(8,8,8,18,18,18,8,8,8,18,18,18), iteration=c(0,0,0,0,0,0,50,50,50,50,50,50), sim.volume=c(1000,1000,2000,1000,1000,2000,1000,1000,2000,1000,1000,2000))
target_annotation <- data.frame(hour=c(8,8,18,18), measurement.at=c(8,8,18,18), iteration=c(0,50,0,50), sim.volume=c(1000,1000,2000,2000))
ggplot(subset(linkstats, (link==1 & (hour==8 | hour==10)) | (link==21 & hour==18)), aes(x=iteration, y=sim.volume)) + geom_point() + scale_y_continuous(limits=c(0, 3000)) + facet_grid(measurement.at~hour) + theme_bw()+ geom_line(data=annotation, color="red", size=0.8, linetype="dashed") + geom_line(data=target_annotation, color="red", size=0.8, linetype="solid")
ggsave("output/illustrative/ill-count-joined.pdf")