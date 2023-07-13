/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package syntheticgen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author Gabriel
 */
public class SyntheticGen {

    private int n;
    private int m;
    private int k;
    
    private HashMap<AbstractMap.SimpleEntry<Integer,Integer>,Integer> map = new HashMap<>();
    
    public SyntheticGen(int n, int m, int k) {
        this.n = n;
        this.m = m;
        this.k = k;
    }

    public int gen(String fnOut) throws Exception {
        int edgeCount = 0;
        int v1, v2 = 0;
        long timestamp;
        
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fnOut)));
        
        Random r = new Random();
        
        while(edgeCount < k) {
            timestamp = 1400000000 - r.nextInt(100000000);
            
            v1 = r.nextInt(n);
            v2 = r.nextInt(m)+n;
            
            //if(map.containsKey(new AbstractMap.SimpleEntry<>(v1,v2)))
            //    continue;
            
            //map.put(new AbstractMap.SimpleEntry<>(v1,v2), k);
            
            //System.out.println("Edge: "+v1+" - "+v2+" "+timestamp);
            out.append(v1+" "+v2+" "+timestamp+"\n");
            
            edgeCount++;
            
            if(edgeCount%1000000 == 0) 
                System.out.println(edgeCount);
        }
        
        out.flush();
        out.close();
        
        return 0;
    }
    
     public int genWeighted(String fnOut) throws Exception {
        int edgeCount = 0;
        int v1, v2 = 0;
        int timestamp;
        
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fnOut)));
        
        Random r = new Random();
        
         while(edgeCount < k) {
            timestamp = 1400000000 - r.nextInt(100000000);
            
            v1 = r.nextInt(n);
            v2 = r.nextInt(m)+n;
            
            //if(map.containsKey(new AbstractMap.SimpleEntry<>(v1,v2)))
            //    continue;
            
            //map.put(new AbstractMap.SimpleEntry<>(v1,v2), k);
            
            //System.out.println("Edge: "+v1+" - "+v2+" "+timestamp);
            out.append(v1+" "+v2+" "+timestamp+"_"+(r.nextInt(5)+1)+"\n");
            
            edgeCount++;
            
            if(edgeCount%1000000 == 0) 
                System.out.println(edgeCount);
        }
        
        out.flush();
        out.close();
        
        return 0;
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SyntheticGen sg = new SyntheticGen(500,500,10000);
        try {
            sg.gen("graph.out"); 
        } catch (Exception e) { 
            e.printStackTrace();
        }

    }
    
}

