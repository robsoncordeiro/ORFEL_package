# The Method ORFEL for Detecting Defamation and Illegitimate Promotion

## Problem
What if a successful company starts to receive a torrent of low-valued (one or two stars) recommendations in its mobile apps from multiple users within a short (say one month) period of time? Is it legitimate evidence that the apps have lost in quality, or an intentional plan (via lockstep behavior) to steal market share through defamation? In the case of a systematic attack to one's reputation, it might not be possible to manually discern between legitimate and fraudulent interaction within the huge universe of possibilities of user-product recommendation. Previous works have focused on this issue, but none of them took into account the context, modeling, and scale that we consider in this work.
	  
## Algorithm
Here, we propose the novel method Online-Recommendation Fraud ExcLuder (ORFEL) to detect defamation and/or illegitimate promotion of online products by using vertex-centric asynchronous parallel processing of bipartite (users-products) graphs. With an innovative algorithm, our results demonstrate both efficacy and efficiency -- over 95% of potential attacks were detected, and ORFEL was at least two orders of magnitude faster than the state-of-the-art. Over a novel methodology, our main contributions are: (1) a new algorithmic solution; (2) one scalable approach; and (3) a novel context and modeling of the problem, which now addresses both defamation and illegitimate promotion.

We expand former works by considering <b>weighted graphs</b>, that is, the weight of the user-product interactions holds semantics that correspond to defamation or to illegitimate promotion on domains ranging from social networks to e-commerce recommendations.

Our work deals with relevant issues of the Web 2.0, potentially augmenting the credibility of online recommendation to prevent losses to both customers and vendors.

## Authors
Gabriel Perri Gimenes, University of São Paulo

Jose F Rodrigues Jr, University of São Paulo

Robson Cordeiro, University of São Paulo

## Download
Please, download the java source with sample data at folder "ORFEL". We also provide our synthetic graph generator and our lockstepper respectively in folders "synthGen" and "Lockstepper" - used to add artificial attacks to the datasets.

<h2>Instructions</h2>
	<ul id="instructions">
	<li><p>To run ORFEL you will need the <a href="https://github.com/GraphChi/graphchi-java">GraphChi library</a>.</p></li>
	<li><p>We also use the <a href="http://labs.carrotsearch.com/hppc.html">HPPC library</a>.</p></li>
	<li><p>The program receives 2 parameters: the input graph (as an edge list text file) and the number of shards to be created - please refer to the GraphChi documentation.</p></li>
	<li><p>In the zip file, we include a synthetic graph with its groundtruth for initial tests.</p></li>
	<li><p>We also include both the weighted and the unweighted versions of the algorithm, so make sure you are using the right one, with the appropriate sample file.</p></li>
	<li><p>Finally, in some of our experiments we used <a href="http://snap.stanford.edu/data/web-FineFoods.html">Amazon.FineFoods</a> and <a href="http://snap.stanford.edu/data/web-Movies.html">Amazon.Movies</a> datasets from <a href="http://snap.stanford.edu/data/web-FineFoods.html">SNAP</a> webpage.
	<li><p>If you have questions or suggestions you can send us an email: <b>ggimenes@icmc.usp.br</b></p></li>
	</ul>
 
## Further details
Further details are available online in the <a href="https://sites.icmc.usp.br/junio/ORFEL/index.html">Project website</a> at the University of São Paulo.
