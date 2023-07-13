/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package lockstepper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import java.util.Random;

/**
 *
 * @author Gabriel
 */
public class Lockstepper {

    /**
     * @param args the command line arguments
     */
    
    public static void main(String[] args) {
        Lockstepper ls = new Lockstepper();
        
        ls.createLockstep(new String[] {"graph.out", "lockstep.out", "20", "10","groundtruth.out", "20"});
        
        //ls.createLockstepWeighted(new String[] {"graph.out", "lockstep.out", "20", "10","groundtruth.out", "20"});
    }
    
    public void createLockstep(String[] args) {
        Random r = new Random();

        String nomeFonte = args[0];
        String nomeDestino = args[1];
        String nomeDestinoGT = args[4];

        int nAttacks = Integer.parseInt(args[5]);
        
        System.out.println("Diretório corrente: " + (new File(".")).getAbsolutePath());

        File fonte = new File(nomeFonte);
        File destino = new File(nomeDestino);
        File destinoGT = new File(nomeDestinoGT);

        System.out.println(fonte.getAbsolutePath());
        System.out.println(destino.getAbsolutePath());

        ArrayList<ArrayList<Integer>> users = new ArrayList<ArrayList<Integer>>();
        ArrayList<ArrayList<Integer>> pages = new ArrayList<ArrayList<Integer>>();
        
        ArrayList<ArrayList<Integer>> usersLines = new ArrayList<ArrayList<Integer>>();
        ArrayList<ArrayList<Integer>> pagesLines = new ArrayList<ArrayList<Integer>>();
        
        for(int z = 0;z<nAttacks;z++) {
            users.add(z,new ArrayList<Integer>());
            pages.add(z,new ArrayList<Integer>());
            usersLines.add(z,new ArrayList<Integer>());
            pagesLines.add(z,new ArrayList<Integer>());
        }
        
        try {
            BufferedReader in = new BufferedReader(new FileReader(fonte));
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(destino,true)));
            PrintWriter out2 = new PrintWriter(new BufferedWriter(new FileWriter(destinoGT,true)));
            
            String buffer;
            String splitted[];
            
            
            //gets the amount of lines in the file
            int lines = 0;

            buffer = in.readLine();
            
            while (buffer != null) {
                lines++;
                buffer = in.readLine();
            }

            System.out.println("Got the number of lines: "+lines);
            
            for(int k=0;k<nAttacks;k++) {
  
                //Randoms the lines for the users and pages
                //Users
                while (usersLines.get(k).size() < Integer.parseInt(args[2])) {
                    usersLines.get(k).add(r.nextInt(lines));
                }

                //pages
                while (pagesLines.get(k).size() < Integer.parseInt(args[3])) {
                    pagesLines.get(k).add(r.nextInt(lines));
                }

            }
                //Reads the file again to get the users and pages
                //Users
                in = new BufferedReader(new FileReader(fonte));
                buffer = in.readLine();
                int i = 0;
                while (buffer != null) {
                    for(int k=0;k<nAttacks;k++) {
                        if (usersLines.get(k).indexOf(i) != -1) {
                            splitted = buffer.split(" ");

                            if (users.get(k).indexOf(Integer.parseInt(splitted[0])) == -1) {
                                users.get(k).add(Integer.parseInt(splitted[0]));
                            }
                        }
                    }
                    i++;
                    buffer = in.readLine();
                }

                //Pages
                in = new BufferedReader(new FileReader(fonte));
                buffer = in.readLine();
                int j = 0;
                while (buffer != null) {
                    for(int k=0;k<nAttacks;k++) {
                        if (pagesLines.get(k).indexOf(j) != -1) {
                            splitted = buffer.split(" ");

                            if (pages.get(k).indexOf(Integer.parseInt(splitted[1])) == -1) {
                                pages.get(k).add(Integer.parseInt(splitted[1]));
                            }
                        }
                    }
                    j++;
                    buffer = in.readLine();
                }

                for(int k=0;k<nAttacks;k++) {
                    
                    for (int a : users.get(k)) {
                        System.out.println(" " + a);
                        out2.append(a + "\n");
                    }

                    System.out.println();

                    for (int a : pages.get(k)) {
                        System.out.println(" " + a);
                    }

                    for (int p : pages.get(k)) {
                        Random rand = new Random();
                        Calendar c = Calendar.getInstance();
                        c.set(2013, 06, rand.nextInt(29) + 1);
                        Date d = c.getTime();

                        int nLikes = 0;

                        for (int u : users.get(k)) {
                            if (r.nextDouble() > 0.05) {
                                float rate = (int) (r.nextDouble() * 5.0);
                                //System.out.println(u+" "+p+" "+new Date().getTime()+" "+rate);
                                out.append(u + " " + p + " " + ((d.getTime() / 1000) + r.nextInt(1000))/*+" "+rate*/ + "\n");
                            }
                        }
                    }
                }
            in.close();
            out.flush();
            out.close();
            out2.flush();
            out2.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();        /*Exibe a pilha, mas continua rodando*/

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void createLockstepWeighted(String[] args) {
        Random r = new Random();

        String nomeFonte = args[0];
        String nomeDestino = args[1];
        String nomeDestinoGT = args[4];

        int nAttacks = Integer.parseInt(args[5]);
        
        System.out.println("Diretório corrente: " + (new File(".")).getAbsolutePath());

        File fonte = new File(nomeFonte);
        File destino = new File(nomeDestino);
        File destinoGT = new File(nomeDestinoGT);

        System.out.println(fonte.getAbsolutePath());
        System.out.println(destino.getAbsolutePath());

        ArrayList<ArrayList<Integer>> vehicles = new ArrayList<ArrayList<Integer>>();
        ArrayList<ArrayList<Integer>> pages = new ArrayList<ArrayList<Integer>>();
        
        ArrayList<ArrayList<Integer>> vehiclesLines = new ArrayList<ArrayList<Integer>>();
        ArrayList<ArrayList<Integer>> pagesLines = new ArrayList<ArrayList<Integer>>();
        
        for(int z = 0;z<nAttacks;z++) {
            vehicles.add(z,new ArrayList<Integer>());
            pages.add(z,new ArrayList<Integer>());
            vehiclesLines.add(z,new ArrayList<Integer>());
            pagesLines.add(z,new ArrayList<Integer>());
        }
        
        try {
            BufferedReader in = new BufferedReader(new FileReader(fonte));
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(destino,true)));
            PrintWriter out2 = new PrintWriter(new BufferedWriter(new FileWriter(destinoGT,true)));
            
            String buffer;
            String splitted[];
            
            
            //gets the amount of lines in the file
            int lines = 0;

            buffer = in.readLine();
            
            while (buffer != null) {
                lines++;
                buffer = in.readLine();
            }

            System.out.println("Got the number of lines: "+lines);
            
            for(int k=0;k<nAttacks;k++) {
  
                //Randoms the lines for the products
                //Users
                while (vehiclesLines.get(k).size() < Integer.parseInt(args[2])) {
                    vehiclesLines.get(k).add(r.nextInt(lines));
                }
                
                //pages
                while (pagesLines.get(k).size() < Integer.parseInt(args[3])) {
                    pagesLines.get(k).add(r.nextInt(lines));
                }
            }
            
                //Reads the file again to get the vehicles
                //Users
                in = new BufferedReader(new FileReader(fonte));
                buffer = in.readLine();
                int i = 0;
                while (buffer != null) {
                    for(int k=0;k<nAttacks;k++) {
                        if (vehiclesLines.get(k).indexOf(i) != -1) {
                            splitted = buffer.split(" ");

                            if (vehicles.get(k).indexOf(Integer.parseInt(splitted[0])) == -1) {
                                vehicles.get(k).add(Integer.parseInt(splitted[0]));
                            }
                        }
                    }
                    i++;
                    buffer = in.readLine();
                }
                
                //Pages
                in = new BufferedReader(new FileReader(fonte));
                buffer = in.readLine();
                int j = 0;
                while (buffer != null) {
                    for(int k=0;k<nAttacks;k++) {
                        if (pagesLines.get(k).indexOf(j) != -1) {
                            splitted = buffer.split(" ");

                            if (pages.get(k).indexOf(Integer.parseInt(splitted[1])) == -1) {
                                pages.get(k).add(Integer.parseInt(splitted[1]));
                            }
                        }
                    }
                    j++;
                    buffer = in.readLine();
                }


                for(int k=0;k<nAttacks;k++) {
                    
                    for (int a : vehicles.get(k)) {
                        System.out.println(" " + a);
                        out2.append(a + "\n");
                    }

                    System.out.println();

                    int timecenter;
                    for (int p : pages.get(k)) {
                        Random rand = new Random();
                        Calendar c = Calendar.getInstance();
                        c.set(2013, 06, rand.nextInt(29) + 1);
                        Date d = c.getTime();

                        int nLikes = 0;

                        for (int u : vehicles.get(k)) {
                            if (r.nextDouble() > 0.05) {
                                
                                //Set the desirable weightspan
                                int weight = 4 + rand.nextInt(2);
                                
                                out.append(u + " " + p + " " + ((d.getTime() / 1000) + r.nextInt(1000))+"_"+weight+ "\n");
                            }
                        }
                    }
                }
            in.close();
            out.flush();
            out.close();
            out2.flush();
            out2.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();       

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
