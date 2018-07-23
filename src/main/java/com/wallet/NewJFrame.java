/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wallet;
import java.math.BigInteger;
import java.io.*;
import java.io.IOException;
import java.lang.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bitcoinj.wallet.DeterministicSeed;
//import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.CipherException;

import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;
import org.web3j.tx.Transfer;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import rx.Subscription;
import java.util.concurrent.Future;
import javax.swing.JOptionPane;
/**
 *
 * @author jack0
 */
public class NewJFrame extends javax.swing.JFrame {
    
    private static DeterministicSeed seed = null;
    private static Web3j web3 = null;
    private static String keystore = new File("").getAbsolutePath()+"/p_chain/chaindata/keystore";//"C:/Users/jack0/Desktop/p_chain/chaindata/keystore";
    private static Subscription ethFinalitySubscription =null;
    private static Subscription ercFinalitySubscription =null;
    private static Subscription ethSubscription =null; 
    private static Subscription ercSubscription =null; 
    private static String tokenAddress = "0x3d480d310ecda960d57fb5246c7231a52ed463b7";
    private static TT_sol_TT Admin_TokenContract = null;
    private static DeterministicKeyChain chain = null;
    private static Credentials adminCredentials = null;
    private static final String adminAddress = "0x72445fcfdeb1fff79496d7ce66089d663ff90e26";
    
    private static void ethSetUp(){
        //Connect to tesnet
        connectTestnet();
        //set up the hd structure
        setBIP();
        //load the token contract instance
        loadAdmin_TokenContract();

    }
    
    private static void connectTestnet(){
        web3 = Web3j.build(new HttpService());  // defaults to http://localhost:8545/
        Web3ClientVersion web3ClientVersion = null;
        try {
            web3ClientVersion = web3.web3ClientVersion().send();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        System.out.println("clientVersion: "+clientVersion);

    }
    private static void setBIP(){
        String seedCode = "yard impulse luxury drive today throw farm pepper survey wreck glass federal";
        try {
            seed = new DeterministicSeed(seedCode,null,"",1409478661L);
        } catch (UnreadableWalletException e) {
            e.printStackTrace();
        }

        chain = DeterministicKeyChain.builder().seed(seed).build();
    }
    private static void loadAdmin_TokenContract(){
        
        String tem = keystore+"/UTC--2018-07-05T02-40-38.446000000Z--72445fcfdeb1fff79496d7ce66089d663ff90e26.json";
        try {
                adminCredentials = WalletUtils.loadCredentials("admin",tem);
            } catch (IOException | CipherException e) {
                e.printStackTrace();
            }      
  
        
        try {
            Admin_TokenContract = TT_sol_TT.load(
                    tokenAddress,web3,adminCredentials,BigInteger.valueOf(200000),BigInteger.valueOf(20000000)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
         //Token Contract Valid
        try {
            if(Admin_TokenContract.isValid()){
                System.out.println("Token Contract is Vaild");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static ECKeyPair generatePair(int index){
        List<ChildNumber> keyPath = HDUtils.parsePath("M/44H/60H/0H/0/"+index);
        DeterministicKey key = chain.getKeyByPath(keyPath, true);
        ECKeyPair childEcKeyPair = ECKeyPair.create(key.getPrivKeyBytes());
        return childEcKeyPair;
    }
    
    private static BigDecimal getEthBalance(String address){
        EthGetBalance balance = null;
        try {
            balance = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return Convert.fromWei(balance.getBalance().toString(),Convert.Unit.ETHER);
    } 
    private static BigDecimal getErcBalance(String address){
        String balance = null;
        try {
            balance = Admin_TokenContract.balanceOf(address).send().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Convert.fromWei(balance,Convert.Unit.ETHER);
    }   
    
    private static boolean unlockAccount(String address){
        Admin web3j = Admin.build(new HttpService());  // defaults to http://localhost:8545/
        PersonalUnlockAccount personalUnlockAccount = null;
        try {
            personalUnlockAccount = web3j.personalUnlockAccount(address, "admin").sendAsync().get();
            return personalUnlockAccount.accountUnlocked();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }  
    }
    
    private static void stopEthFSubscription(){
        if(ethFinalitySubscription!=null)    
            ethFinalitySubscription.unsubscribe();
    }
    private static void stopErcFSubscription(){
        if(ercFinalitySubscription!=null)    
            ercFinalitySubscription.unsubscribe();
    }
    private static void stopEthSubscription(){
        if(ethSubscription!=null)
            ethSubscription.unsubscribe();
    }
    private static void stopErcSubscription(){
        if(ercSubscription!=null)
            ercSubscription.unsubscribe();
    }
    
    private static void ethFinality(int bkNum, String address) throws Exception {

         ethFinalitySubscription = web3.blockObservable(false).subscribe(block -> {
             
              if(block.getBlock().getNumber().intValue()>=bkNum+12){
                //Deposit Success  
                stopEthFSubscription();
                jLabel8.setText(getEthBalance(address)+"   ETH");
                jTextArea1.append("Confirm: "+(block.getBlock().getNumber().intValue()-bkNum)+"\n");
                jTextArea1.append("Deposit Success\n");
              }else{
                 jTextArea1.append("Confirm: "+(block.getBlock().getNumber().intValue()-bkNum)+"\n");
              }

        }, Throwable::printStackTrace);

//        TimeUnit.MINUTES.sleep(2);
//        subscription.unsubscribe();
    } 
    private static void monitorEthDeposit(String address){
        ethSubscription = web3.transactionObservable().subscribe(tx->{
            if(address.equals(tx.getTo())){
//                System.out.println("tx detected: "+tx.getHash()+"Value: "+tx.getValue());
                jLabel8.setText(jLabel8.getText()+" (Receiving "+Convert.fromWei(tx.getValue().toString(),Convert.Unit.ETHER)+" ETH)");
                try {
                    ethFinality(tx.getBlockNumber().intValue(),address);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }
    
    private static void ercFinality(int bkNum, String address) throws Exception {
        ercFinalitySubscription = web3.blockObservable(false).subscribe(block -> {

                if(block.getBlock().getNumber().intValue()>=bkNum+12){
                    //Deposit Success
                    stopErcFSubscription();                    
                    jLabel11.setText(getErcBalance(address)+"   TT");
                    jTextArea1.append("Confirm: "+(block.getBlock().getNumber().intValue()-bkNum)+"\n");
                    jTextArea1.append("Deposit Success\n");
                }else{
                 jTextArea1.append("Confirm: "+(block.getBlock().getNumber().intValue()-bkNum)+"\n");
              }

            }, Throwable::printStackTrace);

    }
    private static void monitorErcDeposit(String address) {
        ercSubscription = Admin_TokenContract.transferEventObservable(
                DefaultBlockParameterName.LATEST,DefaultBlockParameterName.LATEST
                ).subscribe(
                        event->{                           
                            if(event._to.equals(address)){
                                jLabel11.setText(jLabel11.getText()+" (Receiving "+Convert.fromWei(event._value.toString(),Convert.Unit.ETHER)+" TT)");
                 
                                try {
                                    ercFinality(event.log.getBlockNumber().intValue(),address);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
        
    }
    
    private static Credentials getUserCredentials(int userId){
        ECKeyPair userPair = generatePair(userId);
        jTextArea2.append("Received User "+userId+" withdraw request"+"\n");
        
        File f = new File(keystore);
        File[] listOfFiles = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(Keys.getAddress(userPair)+".json");
            }
        }        
        );
        String path = keystore+"/"+listOfFiles[0].getName();
        jTextArea2.append("User "+userId+" Wallet Path:"+path+"\n");
        try {
                return WalletUtils.loadCredentials("admin",path);
            } catch (IOException | CipherException e) {
                e.printStackTrace();
            }   
     
        return null;
    }
    
    private static void generateWallet(ECKeyPair pair){
        File file = new File(keystore);

        try {
            WalletUtils.generateWalletFile("admin", pair, file, false);
        } catch (CipherException | IOException e) {
            e.printStackTrace();
        }
    }
    
    private static Boolean walletIsExit(String address){
        File f = new File(keystore);
        File[] listOfFiles = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(address+".json");
            }
        }        
        );
        
        if(listOfFiles.length>0){
            return true;
        }else{
            return false;
        }
        
    }
    
    
    
    /**
     * Creates new form NewJFrame
     */
    public NewJFrame() {
        initComponents();
        //Admin Account 
        jLabel16.setText(getEthBalance(adminAddress)+"   ETH");
        jLabel18.setText(getErcBalance(adminAddress)+"   TT");     
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jButton6 = new javax.swing.JButton();
        jTextField5 = new javax.swing.JTextField();
        jButton8 = new javax.swing.JButton();
        jTextField4 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel20 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator5 = new javax.swing.JSeparator();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel17 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jSeparator6 = new javax.swing.JSeparator();
        jButton5 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setFont(new java.awt.Font("AGA Arabesque", 0, 18)); // NOI18N
        setPreferredSize(new java.awt.Dimension(1500, 1500));
        setSize(new java.awt.Dimension(1500, 1500));

        jLabel1.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 18)); // NOI18N
        jLabel1.setText("Admin");

        jButton1.setText("Create New Account");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel2.setText("Clients");

        jButton2.setText("Send 10 ETH");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jButton3.setText("Send 10 TT");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel5.setText("Sent To:");

        jButton4.setText("Request Withdraw ETH");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel7.setText("User:");

        jLabel10.setText("Deposit Address:");

        jLabel13.setText("Withdraw To:");

        jTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField3ActionPerformed(evt);
            }
        });

        jLabel9.setText("ETH Balance:");

        jLabel12.setText("TT Balance:");

        jButton6.setText("Request Withdraw TT");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jTextField5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField5ActionPerformed(evt);
            }
        });

        jButton8.setText("switch");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jTextField4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField4ActionPerformed(evt);
            }
        });

        jLabel14.setText("ETH Balance:");

        jLabel15.setText("TT Balance:");

        jLabel16.setText("jLabel16");

        jLabel18.setText("jLabel18");

        jLabel3.setText("Deposit Address:");

        jTextField2.setText("0x72445fcfdeb1fff79496d7ce66089d663ff90e26");
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jLabel4.setText("Log");

        jLabel6.setText("Child ID:");

        jLabel19.setText("Admin Funds");

        jLabel20.setText("Server Log");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane2.setViewportView(jTextArea2);

        jLabel17.setText("Withdraw Amount:");

        jTextField7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField7ActionPerformed(evt);
            }
        });

        jButton5.setText("Refresh");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator2)
                            .addComponent(jSeparator3)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel6)
                                        .addGap(28, 28, 28)
                                        .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(29, 29, 29)
                                        .addComponent(jButton1))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addGap(27, 27, 27)
                                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel14)
                                            .addComponent(jLabel15)
                                            .addGroup(layout.createSequentialGroup()
                                                .addGap(114, 114, 114)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(jLabel16)
                                                    .addComponent(jLabel18))))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButton5)
                                        .addGap(28, 28, 28))))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addGap(20, 20, 20)
                                    .addComponent(jLabel5)
                                    .addGap(18, 18, 18)
                                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(layout.createSequentialGroup()
                                    .addGap(159, 159, 159)
                                    .addComponent(jLabel19)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 341, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(158, 158, 158)
                                .addComponent(jLabel20)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGap(52, 52, 52)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel10)
                                        .addGap(18, 18, 18)
                                        .addComponent(jTextField4))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel9)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel12)
                                                .addGap(18, 18, 18)
                                                .addComponent(jLabel11))
                                            .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGroup(layout.createSequentialGroup()
                                                        .addComponent(jLabel13)
                                                        .addGap(73, 73, 73)
                                                        .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                    .addGroup(layout.createSequentialGroup()
                                                        .addComponent(jLabel17)
                                                        .addGap(48, 48, 48)
                                                        .addComponent(jTextField7)))
                                                .addGap(28, 28, 28)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                    .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                            .addComponent(jSeparator4)
                                            .addComponent(jSeparator6))
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addComponent(jSeparator5)))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 389, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(34, 34, 34)))
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton8)
                                .addGap(168, 168, 168))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(237, 237, 237))))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(27, 27, 27)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jButton1)
                                    .addComponent(jLabel6)
                                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(31, 31, 31)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel3)
                                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel14)
                                    .addComponent(jLabel16))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(17, 17, 17)
                                        .addComponent(jLabel15))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel18)))
                                .addGap(18, 18, 18)
                                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel19)
                                .addGap(10, 10, 10)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel5)))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jButton2)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jButton3)))
                                .addGap(34, 34, 34)
                                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(43, 43, 43)
                                .addComponent(jLabel20)
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(129, 129, 129)
                                .addComponent(jButton5)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(19, 19, 19)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton8))
                        .addGap(18, 18, 18)
                        .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel10)
                                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(28, 28, 28)
                                .addComponent(jLabel9))
                            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(17, 17, 17)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12)
                            .addComponent(jLabel11))
                        .addGap(14, 14, 14)
                        .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel17)
                            .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton4))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13)
                            .addComponent(jButton6))
                        .addGap(18, 18, 18)
                        .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(27, 27, 27))))
            .addComponent(jSeparator1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    //Admin send Erc20
    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        
        try {           
            Admin_TokenContract.transfer(jTextField1.getText(),Convert.toWei("10",Convert.Unit.ETHER).toBigInteger()).sendAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }       
        
    }//GEN-LAST:event_jButton3ActionPerformed
    //Request Withdraw ETH
    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        
        //Check Enough Funds
        BigDecimal money = new BigDecimal(jTextField7.getText());
        BigDecimal bal = new BigDecimal(jLabel8.getText().substring(0, jLabel8.getText().length() - 7));
        int res = bal.compareTo(money);
       
        boolean walletExit = walletIsExit(jTextField4.getText().substring(2));
        
        if(!walletExit){
            JOptionPane.showMessageDialog(this, "This Account is not registered. Press Create Account");
        }
        else if(res == -1){
            JOptionPane.showMessageDialog(this, "Not Enough Funds");
        }else
        {   Credentials userCredentials = getUserCredentials(Integer.parseInt(jTextField5.getText()));
            jTextArea2.append("Credentials Loaded"+"\n");
            TransactionReceipt transactionReceipt=null;
            
            try {
                transactionReceipt = Transfer.sendFunds(
                        web3, userCredentials,jTextField3.getText() ,
                        money, Convert.Unit.ETHER)
                        .send();
                jTextArea2.append("Tranaction has sent\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }//GEN-LAST:event_jButton4ActionPerformed

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField3ActionPerformed
    //Request Withdraw ERC
    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        BigDecimal money = new BigDecimal(jTextField7.getText());
        BigDecimal bal = new BigDecimal(jLabel11.getText().substring(0, jLabel11.getText().length() - 5));
        int res = bal.compareTo(money);
        
        boolean walletExit = walletIsExit(jTextField4.getText().substring(2));
        
        if(!walletExit){
            JOptionPane.showMessageDialog(this, "This Account is not registered. Press Create Account");
        }
        else if(res == -1){
            JOptionPane.showMessageDialog(this, "Not Enough Funds");
        }
        else{
            Credentials userCredentials = getUserCredentials(Integer.parseInt(jTextField5.getText()));
            jTextArea2.append("Credentials Loaded"+"\n");
            TT_sol_TT User_TokenContract = null;
            try {
                User_TokenContract = TT_sol_TT.load(
                        tokenAddress,web3,userCredentials,BigInteger.valueOf(200000),BigInteger.valueOf(20000000)
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
             //Token Contract Valid
            try {
                if(User_TokenContract.isValid()){
                    jTextArea2.append("Token Contract is Vaild"+"\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {           
                User_TokenContract.transfer(jTextField3.getText(),Convert.toWei(jTextField7.getText(),Convert.Unit.ETHER).toBigInteger()).sendAsync().get();
                jTextArea2.append("Tranaction has sent"+"\n");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    //Switch User
    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        stopEthSubscription();
        stopErcSubscription();
        ECKeyPair user = generatePair(Integer.parseInt(jTextField5.getText())); 
        String user_address = "0x"+Keys.getAddress(user);
        jTextField4.setText(user_address);
        jLabel8.setText(getEthBalance(user_address)+"    ETH");
        jLabel11.setText(getErcBalance(user_address)+"   TT");
        monitorEthDeposit(user_address);
        monitorErcDeposit(user_address);
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jTextField4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField4ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField4ActionPerformed

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField2ActionPerformed

//Admin send ETH
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
       
        TransactionReceipt transactionReceipt=null;
        try {
            transactionReceipt = Transfer.sendFunds(
                    web3, adminCredentials,jTextField1.getText() ,
                    BigDecimal.valueOf(10.0), Convert.Unit.ETHER)
                    .send();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButton2ActionPerformed
    //create new account
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        int userId = Integer.parseInt(jTextField6.getText());
        ECKeyPair pair = generatePair(userId);
        
        File f = new File(keystore);
        File[] listOfFiles = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(Keys.getAddress(pair)+".json");
            }
        }        
        );
        
        
        
        if(listOfFiles.length>0){
            JOptionPane.showMessageDialog(this, "The Wallet is exist");
        }else{
            
            generateWallet(pair);
            jTextArea2.append("User "+userId+" 's Wallet has been created"+"\n");
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jTextField5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField5ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField5ActionPerformed

    private void jTextField7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField7ActionPerformed
    //Refresh Admin Balance
    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        
        jLabel16.setText(getEthBalance(adminAddress)+"   ETH");
        jLabel18.setText(getErcBalance(adminAddress)+"   TT");  
    }//GEN-LAST:event_jButton5ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        ethSetUp();    
        
        
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton8;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private static javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private static javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private static javax.swing.JTextArea jTextArea1;
    private static javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private static javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    // End of variables declaration//GEN-END:variables
}
