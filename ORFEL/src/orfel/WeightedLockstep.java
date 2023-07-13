/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package orfel;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.ObjectArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.IntIntCursor;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.carrotsearch.hppc.sorting.IndirectSort;
import edu.cmu.graphchi.ChiFilenames;
import edu.cmu.graphchi.ChiLogger;
import edu.cmu.graphchi.ChiVertex;
import edu.cmu.graphchi.GraphChiContext;
import edu.cmu.graphchi.GraphChiProgram;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.datablocks.LongConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author Gabriel
 */
public class WeightedLockstep implements GraphChiProgram<Integer, EdgeType> {
    
     final static int DELTA_T = 602400;
     
         
     private static Logger logger = ChiLogger.getLogger("Weighted");
     private int nUsers, nPages, nIsles, minVehicles, minUsers, nClusters, iterConvergence = 0;
     float userPercent;
     
     private String basefilename;
     
     // Multicluster model
     //private ArrayList<ArrayList<Integer>> lsCPages; 
     //private ArrayList<ArrayList<Integer>> lsCUsers;
     private ObjectArrayList<IntArrayList> lsCPages;
     private ObjectArrayList<IntArrayList> lsCUsers;
     
     
     //aux to analyze convergence
     private ArrayList<ArrayList<Integer>> oldLsCPages; 
     private ArrayList<ArrayList<Integer>> oldLsCUsers;
     private ArrayList<ArrayList<Integer>> currentLsCPages; 
     private ArrayList<ArrayList<Integer>> currentLsCUsers;
     
     //aux to see which clusters changed
     //private ArrayList<Integer> cChangedUsers;
     //private ArrayList<Integer> cChangedPages;
     private IntArrayList cChangedUsers;
     private IntArrayList cChangedPages;
     
     //private ArrayList<LinkedHashMap<Integer,LinkedHashMap<Integer,Integer>>> lsCmaps;
     private ObjectArrayList<IntObjectOpenHashMap<IntObjectOpenHashMap<EdgeType>>> lsCmaps;
     

    //receives clustering params 
    public WeightedLockstep(int nClusters,int minVehicles, float userPercent, String basefilename) {
        this.nPages = 0;
        this.nUsers = 0;
        this.nIsles = 0;
        this.minVehicles = minVehicles;
        this.minUsers = 30;
        this.userPercent = userPercent;
        this.nClusters = nClusters;
        this. basefilename = basefilename;
        
        this.cChangedUsers = new IntArrayList();
        this.cChangedPages = new IntArrayList();
        
        //multicluster model
        lsCPages = new ObjectArrayList<>(0);
        for (int c = 0;c<nClusters;c++) {
            lsCPages.add(new IntArrayList());
        }
        
        lsCUsers = new ObjectArrayList<>(0);
        for (int c = 0;c<nClusters;c++) {
            lsCUsers.add(new IntArrayList());
        }
         
        lsCmaps = new ObjectArrayList<IntObjectOpenHashMap<IntObjectOpenHashMap<EdgeType>>>(0);
        for (int c = 0;c<nClusters;c++) {
            lsCmaps.add(new IntObjectOpenHashMap<IntObjectOpenHashMap<EdgeType>>());
        }
    }
    
     @Override
    public void update(ChiVertex<Integer, EdgeType> vertex, GraphChiContext context) {
        final int iteration = context.getIteration();
        int j = 0;
        
        ArrayList<Integer> nUsersSampled = new ArrayList<>();

        
        float intersection;
        Random r = new Random();

        
        //initializes the seeds
        if (iteration == 0) {
            for (int k =0;k<this.nClusters;k++)
                nUsersSampled.add(0);
            for(int c = 0; c < this.nClusters;c++) {             
                if (vertex.getId() == lsCPages.get(c).get(0)) {
                    lsCmaps.get(c).put(lsCPages.get(c).get(0),new IntObjectOpenHashMap());
                    for (int i = 0; i < vertex.numInEdges(); i++) {

                        //randomly sampling the users
                        if (nUsersSampled.get(c) <= 50 && (r.nextDouble() < 0.3)) {
                            lsCUsers.get(c).add(vertex.edge(i).getVertexId());
                            lsCmaps.get(c).get(lsCPages.get(c).get(0)).put(vertex.edge(i).getVertexId(), vertex.edge(i).getValue());
                            nUsersSampled.set(c, nUsersSampled.get(c)+1);
                        }
                    }
                }
            }
        } else if (iteration > 0) {

            //checks if its update center or update subspace iteration
            if (iteration % 2 == 1) {

                //Pages
                if (vertex.numInEdges() > 0) {
                    //checks if the minPages has already been achieved
                    for (int c = 0; c < this.nClusters; c++) {

                        //checks if the page is already on the cluster, proceeds if not
                        if (lsCPages.get(c).indexOf(vertex.getId()) == -1) {
                            HashMap<Integer, EdgeType> tempMap = new HashMap<Integer, EdgeType>();

                            //for each like in the candidate page, checks if it is in the cluster
                            long tempTimecenter = 0;
                            intersection = 0;

                            for (int i = 0; i < vertex.numInEdges(); i++) {
                                if ((lsCUsers.get(c).indexOf(vertex.edge(i).getVertexId()) != -1) && weightConstraint(vertex.edge(i).getValue().weight)) {
                                    intersection++;
                                    tempTimecenter += vertex.edge(i).getValue().year;
                                }
                            }

                            // if at least ro users like the page, check the timecenter
                            tempTimecenter = (long) (tempTimecenter / intersection);
                            if ((intersection / lsCUsers.get(c).size()) >= (this.userPercent)) {

                                //for each like in the cluster, checks if it is within the determined timecenter
                                intersection = 0;
                                for (int i = 0; i < vertex.numInEdges(); i++) {
                                    if ((lsCUsers.get(c).indexOf(vertex.edge(i).getVertexId()) != -1) 
                                            && ((Math.abs(vertex.edge(i).getValue().year - tempTimecenter) < 2 * DELTA_T)) 
                                                && (weightConstraint(vertex.edge(i).getValue().weight))) {
                                        
                                        intersection++;
                                        tempMap.put(vertex.edge(i).getVertexId(), vertex.edge(i).getValue());
                                    }
                                }
                            }

                            if (lsCPages.get(c).size() < this.minVehicles) {
                                //if less than userPercent users in the cluster dont like the candidate page - then its a valid page to add
                                if ((intersection / lsCUsers.get(c).size()) >= (this.userPercent)) {
                                    //Add the page to the cluster
                                    lsCPages.get(c).add(vertex.getId());
                                    for (Map.Entry<Integer, EdgeType> entry : tempMap.entrySet()) {
                                        if(lsCmaps.get(c).get(vertex.getId()) == null) {
                                            lsCmaps.get(c).put(vertex.getId(), new IntObjectOpenHashMap());
                                        }
                                        lsCmaps.get(c).get(vertex.getId()).put(entry.getKey(), entry.getValue());
                                    }

                                    //optimization try
                                    if (this.cChangedPages.indexOf(c) == -1) {
                                        this.cChangedPages.add(c);
                                    }

                                    //logger.log(Level.INFO, "Adicionando a página: {0} no cluster com divergencia de {1}", new Object[]{vertex.getId(), intersection / lsCUsers.get(c).size()});
                                }

                            } else {
                                //Check if the candidate page is better than one of the current pages on the cluster

                                for (IntObjectCursor<IntObjectOpenHashMap<EdgeType>> cursor : lsCmaps.get(c)) {
                                    // If the candidate page has more likes than the current page, swap them
                                    if (cursor.value !=null && cursor.value.size() < intersection) {
                                        //Removing the "bad" page from lscomm
                                        lsCPages.get(c).removeAllOccurrences(cursor.key);

                                        //Adding the new page to lscomm
                                        lsCPages.get(c).add(vertex.getId());

                                        //Removing the bad page from maps
                                        lsCmaps.get(c).remove(cursor.key);

                                        //Adding the new page to maps
                                        
                                        for (Map.Entry<Integer, EdgeType> entry2 : tempMap.entrySet()) {
                                            if(lsCmaps.get(c).get(vertex.getId()) == null) {
                                                lsCmaps.get(c).put(vertex.getId(), new IntObjectOpenHashMap());
                                            }
                                            lsCmaps.get(c).get(vertex.getId()).put(entry2.getKey(), entry2.getValue());
                                        }

                                        //optimization try
                                        if (this.cChangedPages.indexOf(c) == -1) {
                                            this.cChangedPages.add(c);
                                        }

                                            //logger.info("Swapping page: " + context.getVertexIdTranslate().backward(tempKeyList.get(i)) + " for page: " + context.getVertexIdTranslate().backward(vertex.getId()));
                                        //stop trying to compare the candidate page
                                        break;
                                    }
                                }
                            }

                        }

                    }
                }
                // Users iteration (even ones)
            } else {
                
                //Users
                if (vertex.numOutEdges() > 0) {
                    
                    //checks if the user is already on the cluster, proceeds if not
                    for(int c = 0; c< this.nClusters;c++) {

                        if (lsCUsers.get(c).indexOf(vertex.getId()) == -1) {
                            
                            //for each like of the user, checks if its in the cluster within deltaT of the current timecenter for that page
                            HashMap<Integer, EdgeType> tempMap = new HashMap<Integer, EdgeType>();
                            intersection = 0;
                            long pageCenter;
                            

                            for (int i = 0; i < vertex.numOutEdges(); i++) {
                                if (lsCPages.get(c).indexOf(vertex.edge(i).getVertexId())!=-1) {
                                    pageCenter = this.timecenter(c, vertex.edge(i).getVertexId());
                                
                                    if ((((Math.abs(vertex.edge(i).getValue().year - pageCenter)) < 4 * DELTA_T) || pageCenter == 0) && (weightConstraint(vertex.edge(i).getValue().weight))) {
                                        intersection++;
                                        tempMap.put(vertex.edge(i).getVertexId(), vertex.edge(i).getValue());
                                        //logger.info("Page " + page + " is liked by user " + vertex.getId() + " within " + Math.abs(vertex.edge(i).getValue() - this.timecenter(c,page.intValue())));
                                    }
                                }

                            }
                            //if the user likes at least userPercent pages in the cluster, he can be added
                            if ((intersection / lsCPages.get(c).size()) >= (this.userPercent)) {

                                //Add the user to the cluster
                                lsCUsers.get(c).add(vertex.getId());
                                for (Map.Entry<Integer, EdgeType> entry : tempMap.entrySet()) {
                                    if(lsCmaps.get(c).get(entry.getKey()) == null) {
                                        lsCmaps.get(c).put(entry.getKey(), new IntObjectOpenHashMap());
                                    }
                                    lsCmaps.get(c).get(entry.getKey()).put(vertex.getId(), entry.getValue());
                                }
                                
                                //optimization try
                                if(this.cChangedUsers.indexOf(c) == -1)
                                    this.cChangedUsers.add(c);
                                
                                //logger.log(Level.INFO, "Adicionando o usuário: {0} no cluster com intersec de {1}", new Object[]{vertex.getId(), intersection / lsCPages.get(c).size()});
                            }
                        }
                    }
                }
            }
        }
    }

     
     
         /**
     * Initialize the sharder-program.
     * @param graphName
     * @param numShards
     * @return
     * @throws java.io.IOException
     */
    protected static FastSharder createSharder(String graphName, int numShards) throws IOException {
        return new FastSharder<Integer, EdgeType>(graphName, numShards, new VertexProcessor<Integer>() {
            @Override
            public Integer receiveVertexValue(int vertexId, String token) {
                return 0;
            }
        }, new EdgeProcessor<EdgeType>() {
            @Override
            public EdgeType receiveEdge(int from, int to, String token) {
                String s[] = token.split("_");
                if(token.length() == 0) 
                    return new EdgeType(0,0);
                return new EdgeType(Integer.parseInt(s[0]),Integer.parseInt(s[1]));
            }
        }, new IntConverter(), new EdgeTypeConverter());
    }
    
    
    public void beginIteration(GraphChiContext ctx) {
        
        //seeds
        if(ctx.getIteration() == 0) {
            int c = 0;
            for(Integer i : this.samplePageSeeds(this.basefilename)) {
                lsCPages.get(c).add(ctx.getVertexIdTranslate().forward(i));
                c++;
            }
            
            //populates the cchanged for optimization (0th iter)
            for(int j = 0;j<this.nClusters;j++) {
                this.cChangedUsers.add(j);
            }
        }
       
        if(ctx.getIteration() == 0) {
            for(int c = 0; c< this.nClusters;c++) {
                logger.info("CLUSTER "+c+" -----------------------------------------------------------------");
                for (int i=0;i<lsCPages.get(c).size();i++) {
                    logger.log(Level.INFO, "Página: {0} no cluster.", ctx.getVertexIdTranslate().backward(lsCPages.get(c).get(i)));
                }

                for (int j=0;j<lsCUsers.get(c).size();j++) {
                    logger.log(Level.INFO, "Usuário: {0} no cluster.", ctx.getVertexIdTranslate().backward(lsCUsers.get(c).get(j)));
                }
            }
        }
        
        if(ctx.getIteration() % 2 == 0 && ctx.getIteration()!=0) {
            this.cChangedUsers = new IntArrayList();
          
            
            logger.info("Clusters to be worked on: "+this.cChangedPages.size());
        }
        
        if(ctx.getIteration() % 2 == 1 && ctx.getIteration()!=0) {
              this.cChangedPages = new IntArrayList();
                
            
            logger.info("Clusters to be worked on: "+this.cChangedUsers.size());

        }
    }

    public void endIteration(GraphChiContext ctx) {
        
        logger.info("Start of enditeration.");
        
        
        // sampling of users on the first 2 user iterations
        if (ctx.getIteration()%2 == 0 && ctx.getIteration() <= 4 && ctx.getIteration() > 0) {
            for(int c = 0;c<nClusters;c++)
                this.sampleUsers(c);
        }
        
        // for each cluster, running the updatecenter method - finds the best subset delta_t s.t maximizes likes and removes the users that are not on the subset
        if(ctx.getIteration() % 2 == 0) {
            for(int c = 0;c<this.nClusters;c++) {
                //changes only the clusters that changed
                if(cChangedUsers.indexOf(c) != -1) {
                    logger.info("Running update center for cluster: "+c);
                    updatecenter(c, ctx);
                }
            }          
        }
        
        
        if(ctx.getIteration() % 2 == 0) {
        try {
            //checks convergence NOT WORKING - due to structures not being serializable
            if (ctx.getIteration() == 0) {
                FileOutputStream fout = new FileOutputStream("pagesConv.ser");
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                
                oos.writeObject(serializar(lsCPages));

                fout = new FileOutputStream("usersConv.ser");
                oos = new ObjectOutputStream(fout);
                oos.writeObject(serializar(lsCUsers));
            } else {

                FileInputStream fin = new FileInputStream("pagesConv.ser");
                ObjectInputStream ois = new ObjectInputStream(fin);
                oldLsCPages = (ArrayList<ArrayList<Integer>>) ois.readObject();

                fin = new FileInputStream("usersConv.ser");
                ois = new ObjectInputStream(fin);
                oldLsCUsers = (ArrayList<ArrayList<Integer>>) ois.readObject();

                if (lsCPages.equals(Dserializar(oldLsCPages)) && lsCUsers.equals(Dserializar(oldLsCUsers))) {
                    //convergiu
                    if (this.iterConvergence == 0) {
                        this.iterConvergence = ctx.getIteration();
                        logger.info("Convergiu na iteração: "+ctx.getIteration());
                    }
                } else {
                    FileOutputStream fout = new FileOutputStream("pagesConv.ser");
                    ObjectOutputStream oos = new ObjectOutputStream(fout);
                    oos.writeObject(serializar(lsCPages));

                    fout = new FileOutputStream("usersConv.ser");
                    oos = new ObjectOutputStream(fout);
                    oos.writeObject(serializar(lsCUsers));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        }
        logger.info("End of enditeration.");
    }

   

    public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}

    public void endInterval(GraphChiContext ctx, VertexInterval interval) {}

    public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}

    public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        String baseFilename = args[0];
        int nShards = Integer.parseInt(args[1]);
       

        /* Create shards */
        FastSharder sharder = createSharder(baseFilename, nShards);
        if (baseFilename.equals("pipein")) {     // Allow piping graph in
            sharder.shard(System.in, FastSharder.GraphInputFormat.EDGELIST);
        } else {
            if (!new File(ChiFilenames.getFilenameIntervals(baseFilename, nShards)).exists()) {
                sharder.shard(new FileInputStream(new File(baseFilename)), FastSharder.GraphInputFormat.EDGELIST);
            } else {
                logger.info("Found shards -- no need to preprocess");
            }
        }

        /* Run ORFEL - nSeeds,m,rho */
        WeightedLockstep gc = new WeightedLockstep(1000,20,0.8f,baseFilename);
        
        GraphChiEngine<Integer, EdgeType> engine = new GraphChiEngine<Integer, EdgeType>(baseFilename, nShards);
        engine.setEdataConverter(new EdgeTypeConverter());
        engine.setVertexDataConverter(null);
        //engine.setEnableScheduler(true);
        
        //Testing mods
        engine.setModifiesInedges(false);
        engine.setModifiesOutedges(false);
        engine.setEnableDeterministicExecution(false);
        
        
        engine.run(gc, 11);

        logger.info("Ready. Finished.");
        //logger.log(Level.INFO, "nNodes: {2} nEdges: {3} nIsles: {4} nUsers: {0} nPages: {1}", new Object[]{gc.nUsers, gc.nPages, engine.numVertices(), engine.numEdges(), gc.nIsles});

        //logger.info("TRADUCAO= "+engine.getVertexIdTranslate().forward(3538));
        
        
        
        //Imprime os clusters e suas informações
        int goodClusters = 0;
        for(int c = 0;c<gc.nClusters;c++) {
            
            if(gc.lsCPages.get(c).size() == gc.minVehicles && gc.lsCUsers.get(c).size() >= gc.minUsers) {
                goodClusters++;
                logger.info("CLUSTER "+c+" -----------------------------------------------------------------");
            

            for (int i=0;i<gc.lsCPages.get(c).size();i++) {
                logger.log(Level.INFO, "Página: {0} no cluster.", engine.getVertexIdTranslate().backward(gc.lsCPages.get(c).get(i)));
            }

            for (int j=0;j<gc.lsCUsers.get(c).size();j++) {
                logger.log(Level.INFO, "Usuário: {0} no cluster.", engine.getVertexIdTranslate().backward(gc.lsCUsers.get(c).get(j)));
            }
            
            }
        }
        
        logger.info("\n");
        logger.info("The percentage of good clusters was: "+goodClusters+" / "+gc.nClusters+" = "+(double)goodClusters/gc.nClusters);
        logger.info("\n");
        logger.info("The percentage of users caught was: "+gc.checkResults("R"+gc.basefilename,engine));
        logger.info("Convergiu na iteração: "+gc.iterConvergence);
    }
    
    private long timecenter(int cluster,int page){
        long sum = 0;
        //logger.info("Likestamps da pagina: "+page);
        if(this.lsCmaps.get(cluster).get(page) != null && this.lsCmaps.get(cluster).get(page).size()>0) {
              
                for(IntObjectCursor<EdgeType> cursor : this.lsCmaps.get(cluster).get(page)) {
                    if(cursor.value != null)
                        sum += cursor.value.year;
                }

                return sum/this.lsCmaps.get(cluster).get(page).size();
            
        }
        return 0;
    }
    
    private int getLikes(int cluster,int user) {
        int sum = 0;
 
        for (IntObjectCursor<IntObjectOpenHashMap<EdgeType>> cursor : this.lsCmaps.get(cluster)) {
            if (cursor.value.containsKey(user))
                //sum+= cursor.value.get(user).weight;
                sum++;
        }
        return sum;
    }
    
    private void removeUser(int cluster,int user) {
        for (IntObjectCursor<IntObjectOpenHashMap<EdgeType>> cursor : this.lsCmaps.get(cluster)) {
            cursor.value.remove(user);
        }
        lsCUsers.get(cluster).removeAllOccurrences(user);
    }
    
    private void sampleUsers(int cluster) {
        Random r =  new Random();
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        for (IntObjectCursor<IntObjectOpenHashMap<EdgeType>> cursor : this.lsCmaps.get(cluster)) {
            for (IntObjectCursor<EdgeType> cursor2 : cursor.value) {
                if (r.nextDouble()<0.3) {
                    toRemove.add(cursor2.key);
                }
            }
        }
        
        for (Integer user : toRemove) {
            this.removeUser(cluster,user);
        }
    }
    
    private ArrayList<Integer> samplePageSeeds(String filename) {
        Random r = new Random();

        String nomeFonte = filename;

        File fonte = new File(nomeFonte);

        ArrayList<Integer> pages = new ArrayList<Integer>();
        ArrayList<Integer> pageLines = new ArrayList<Integer>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(fonte));

            String buffer;
            String splitted[];
            
            //gets the amount of lines in the file
            int lines = 0;

            buffer = in.readLine();
            
            while (buffer != null) {
                lines++;
                buffer = in.readLine();
            }

            //Randoms the lines for the pages
            int l;
            while(pageLines.size() < this.nClusters) {
                l = r.nextInt(lines);
                if(pageLines.indexOf(l)==-1)
                    pageLines.add(l);
            }
            
            //gets the page ids for the randomed pagelines
            in = new BufferedReader(new FileReader(fonte));
            buffer = in.readLine();
            int j = 0;
            while (buffer != null) {
                splitted = buffer.split(" ");

                if(pageLines.indexOf(j)!=-1) {
                    pages.add(Integer.parseInt(splitted[1]));
                }
                j++;
                buffer = in.readLine();
           }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return pages;
    }
    
     private void updatecenter(int c, GraphChiContext ctx) {
        Random r = new Random();
        
        //Maximize likes within deltaT for each of the dimensions
         
        //sorting for each of the dimensions (pages)
        for (IntObjectCursor<IntObjectOpenHashMap<EdgeType>> cursor : this.lsCmaps.get(c)) {
            List<AbstractMap.SimpleEntry<Integer, Integer>> entries = new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>();
            
            for(IntObjectCursor<EdgeType> cursor2 : cursor.value) {
                if(cursor2.value != null)
                    entries.add(new AbstractMap.SimpleEntry<Integer,Integer>(cursor2.key,cursor2.value.year));
            }
            
            Collections.sort(entries, new Comparator<AbstractMap.SimpleEntry<Integer,Integer>>() {
                public int compare(AbstractMap.SimpleEntry<Integer,Integer> a,AbstractMap.SimpleEntry<Integer,Integer> b) {
                    return a.getValue().compareTo(b.getValue());
                }
            });
            
            Map<Integer, Integer> sortedMap = new LinkedHashMap<Integer, Integer>();
            for (AbstractMap.SimpleEntry<Integer,Integer> entry2 : entries) {
                sortedMap.put(entry2.getKey(), entry2.getValue());
            }
            
            // Calculates the best subset deltaT - s.t. maximizes likes within P'
            int startSubSet, endSubSet = 0;
            long subsetCenter = 0;
            int likesSum = 0;
            int currentTopSum = 0;
            int currentStart = 0, currentEnd = 0;
            long currentCenter = 0;
            for (Map.Entry<Integer, Integer> entry3 : sortedMap.entrySet()) {
                startSubSet = 0;
                endSubSet = 0;
                likesSum = 0;
                subsetCenter = 0;
                for (Map.Entry<Integer, Integer> entry4 : sortedMap.entrySet()) {
                    if (Math.abs(entry3.getValue() - entry4.getValue()) < 2 * DELTA_T) {
                        endSubSet++;
                        likesSum += this.getLikes(c,entry4.getKey());
                        //likesSum += r.nextInt(50);
                        subsetCenter += entry4.getValue();
                        
                        if (currentTopSum < likesSum) {
                            currentTopSum = likesSum;
                            currentStart = startSubSet;
                            currentEnd = endSubSet;
                            currentCenter = subsetCenter;
                        }
                    } else {
                        break;
                    }
                    
                }
            }
            
            //Calculates the center of the best subset
            double bestCenter = 0;
            if (currentEnd != currentStart) {
                bestCenter = (currentCenter / (currentEnd - currentStart));
            } else {
                bestCenter = 0;
            }
            
            //Removes the users that are not in the subset
            ArrayList<Integer> toRemove = new ArrayList<>();
            for(IntObjectCursor<EdgeType> cursor2 : cursor.value) {
                if(cursor2.value != null) {
                    if (!(Math.abs(cursor2.value.year - bestCenter) < 2 * DELTA_T)) {
                        toRemove.add(cursor2.key);
                        logger.info("Removendo o usuário: " + ctx.getVertexIdTranslate().backward(cursor2.key));
                    }
                }
            }
            
            for (Integer v : toRemove) {
                this.removeUser(c,v);
            }
        }
    }
     
    private double checkResults (String filename, GraphChiEngine<Integer, EdgeType> eg) {
        String nomeFonte = filename;

        File fonte = new File(nomeFonte);
        
        int usersCaught = 0;
        int totalSuspectUsers = 0;

        try {
            BufferedReader in = new BufferedReader(new FileReader(fonte));

            String buffer;

            buffer = in.readLine();

            while (buffer != null) {
                for(int c = 0;c<this.nClusters;c++) {
                    if(this.lsCUsers.get(c).size() >= this.minVehicles && this.lsCPages.get(c).size() >= this.minVehicles
                            && this.lsCUsers.get(c).indexOf(eg.getVertexIdTranslate().forward(Integer.parseInt(buffer))) != -1) {
                        usersCaught++;
                        break;
                    }
                }

                totalSuspectUsers++;
                buffer = in.readLine();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return usersCaught/(float)totalSuspectUsers;
    }
    
    //Defines how the weight is used to form locksteps
    private boolean weightConstraint(int weight) {
         return (weight < 3);
    }
    
    private ArrayList<ArrayList<Integer>> serializar(ObjectArrayList<IntArrayList> other) {
        ArrayList<ArrayList<Integer>> aux = new ArrayList<ArrayList<Integer>>();
        
        for(int c = 0; c < this.nClusters;c++) {
            aux.add(new ArrayList<Integer>());
            for(int i = 0;i<other.get(c).size();i++)
                aux.get(c).add(other.get(c).get(i));
        }
         
        return(aux);
    }
    
    private ObjectArrayList<IntArrayList> Dserializar(ArrayList<ArrayList<Integer>> other) {
       ObjectArrayList<IntArrayList> aux = new ObjectArrayList<IntArrayList>();
        
        for(int c = 0; c < this.nClusters;c++) {
            aux.add(new IntArrayList());
            for(int i = 0;i<other.get(c).size();i++)
                aux.get(c).add(other.get(c).get(i));
        }
         
        return(aux);
    }
}
