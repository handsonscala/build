Merge-sort implementation parallelized using Futures

```bash
amm TestMergeSort.sc
```

```diff
6.2 - GenericMergeSort
```

The cutoff between sequential and parallel sorting is arbitrary; benchmarks show
the optimal cutoff to be somewhere around 2-32 items:

| cutover | parallel     | sequential   | speedup |
|--------:|-------------:|-------------:|--------:|
| 1       |       1995ms |       3503ms | 1.76    |
| 2       |       1383ms |       3480ms | 2.52    |
| 4       |       1491ms |       3607ms | 2.42    |
| 8       |       1695ms |       3660ms | 2.16    |
| 16      |       1332ms |       3357ms | 2.52    |
| 32      |       1391ms |       3253ms | 2.34    |
| 64      |       1645ms |       3312ms | 2.01    |
| 128     |       1645ms |       3312ms | 2.10    |
| 256     |       2000ms |       3647ms | 2.10    |
