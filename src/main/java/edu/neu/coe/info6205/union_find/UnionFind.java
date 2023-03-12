package edu.neu.coe.info6205.union_find;

import java.util.*;

public class UnionFind {
    int totalConnectedCount = 0;
    private void updateTotalConnectedCount (){
       totalConnectedCount++;
    }
    public static void main(String[] args) {
        for (int site = 2500; site <= 100000; site+=2500) {
            int count = UnionFind.count(site);
            System.out.printf("Site: %8d, connection: %8d, nlogn: %8d\n", site,count,  (int)(site * (Math.log10(site))));
        }
    }
    private static int count(int pairsNum) {
        int averageConnectedCount = 0;
        int runs = 200;
        for (int i = 0; i < runs; i++) {
            int sites = pairsNum;
            UnionFind uf = new UnionFind();
            UF_HWQUPC uf_hwqupc = new UF_HWQUPC(sites, false);
            Random random = new Random();
            while (uf_hwqupc.components() != 1) {
                int node1 = random.nextInt(pairsNum);
                int node2 = random.nextInt(pairsNum);
                uf.updateTotalConnectedCount();
                if (node1 == node2) {
                    continue;
                }
                if (!uf_hwqupc.isConnected(node1, node2)) {
                    uf_hwqupc.union(node1, node2);
                }
            }
            averageConnectedCount+=uf.totalConnectedCount;
        }
        return averageConnectedCount/runs;
    }
}
