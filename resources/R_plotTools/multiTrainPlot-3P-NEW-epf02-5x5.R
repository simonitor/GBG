# 
# This plot shows for 5x5 Hex different TD-update scheme forms. The first result is, that the NEW_2P 
# form is shown to be fully equivalent to (NEW_3P, 1-ply)-form, as it should. The 2nd result is 
# that the (NEW_3P, 2-ply)-form is significantly better, especially if we look at evalMode=10 (diff. 
# start states, tgc2). 
#
# More details are found in TR-TDNTuple.tex.
# 
library(ggplot2)
library(grid)
source("summarySE.R")

path <- "../../agents/Hex/05/csv/"; limits=c(0.0,1.0); errWidth=3000;

filenames=c ( "multiTrain-25-6-lam050-epf02.csv"
              ,"multiTrain-25-6-lam050-epf02-3P-1ply.csv"    # 10 runs = 2.8h
              ,"multiTrain-25-6-lam050-epf02-3P-2ply.csv"    # 10 runs = 2.8h
              ,"multiTrain-25-6-lam075-epf02.csv"
            )
# other pars: alpha=0.2, epsilon = 1.0 ... 0.2, ChooseStart01=T, LearnFromRM=F, 
# 10 runs, 150.000 training episodes. 
# evalMode= 0 (evalQ) is from default start state against MCTS, 
# evalMode=10 (evalT) is from different start states against MCTS. 
 
PLOTALLLINES=F    # if =T: make a plot for each filename, with one line for each run
  
dfBoth = data.frame()
for (k in 1:length(filenames)) {
  filename <- paste0(path,filenames[k])
  df <- read.csv(file=filename, na.string="-1", dec=".",skip=2)
  df$run <- as.factor(df$run)
  df <- df[,setdiff(names(df),"trnMoves")]
  #if (k==1) df <- cbind(df,actionNum=rep(0,nrow(df)))
  
  if (PLOTALLLINES) {
    q <- ggplot(df,aes(x=gameNum,y=evalQ))
    q <- q+geom_line(aes(colour=run),size=1.0) + 
      scale_y_continuous(limits=c(-1,0)) #+
      #facet_grid(.~THETA, labeller = label_both)
    plot(q)
  }
  
  lambdaCol = switch(k
                    ,rep("0.50 2P",nrow(df))
                    ,rep("0.50 3P-1ply",nrow(df))
                    ,rep("0.50 3P-2ply",nrow(df))
                    ,rep("0.75 2P",nrow(df))
  )
  #browser()
  dfBoth <- rbind(dfBoth,cbind(df,lambda=lambdaCol))
                  
}

tgc <- data.frame()
# summarySE is a very useful script from www.cookbook-r.com/Graphs/Plotting_means_and_error_bars_(ggplot2)
# It summarizes a dataset, by grouping measurevar according to groupvars and calculating
# its mean, sd, se (standard dev of the mean), ci (conf.interval) and count N.
tgc1 <- summarySE(dfBoth, measurevar="evalQ", groupvars=c("gameNum","lambda"))
tgc1 <- cbind(tgc1,evalMode=rep(0,nrow(tgc1)))
names(tgc1)[4] <- "eval"  # rename "evalQ"
tgc2 <- summarySE(dfBoth, measurevar="evalT", groupvars=c("gameNum","lambda"))
tgc2 <- cbind(tgc2,evalMode=rep(10,nrow(tgc2)))
names(tgc2)[4] <- "eval"  # rename "evalT"
tgc <- rbind(tgc1,tgc2)
#tgc <- tgc1
tgc <- tgc2
tgc$lambda <- as.factor(tgc$lambda)
tgc$evalMode <- as.factor(tgc$evalMode)

# The errorbars may overlap, so use position_dodge to move them horizontally
pd <- position_dodge(3000) # move them 300 to the left and right

q <- ggplot(tgc,aes(x=gameNum,y=eval,colour=lambda,linetype=evalMode))
q <- q+geom_errorbar(aes(ymin=eval-se, ymax=eval+se), width=errWidth, position=pd)
q <- q+geom_line(position=pd,size=1.0) + geom_point(position=pd,size=2.0) 
q <- q+scale_y_continuous(limits=limits) 
#q <- q+guides(colour = guide_legend(reverse = TRUE))
plot(q)

