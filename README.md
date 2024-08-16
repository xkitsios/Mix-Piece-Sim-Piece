# Mix-Piece / Sim-Piece
*Sim-Piece: Highly Accurate Piecewise Linear Approximation through Similar Segment Merging*

## Description
Sim-Piece is a time-series lossy data compressor influenced by PLA. If you are interested in lossless time-series compression you can take a look at our CHIMP algorithm available at https://github.com/panagiotisl/chimp.

Sim-Piece utilizes a user-defined maximum absolute error bound to generate a remarkably compact binary representation of a time series. Sim-Piece is particularly suitable for applications that need to accommodate large time series datasets even when the acceptable error threshold is very small.

The algorithm consists of two stages. During the first stage, an intermediate angle-based PLA representation is constructed. This involves generating disjoint PLA segments, each accompanied by a quantized starting point and a range of allowable slopes, determined by the error threshold. In the second stage, these intermediate segments are merged and jointly represented in the final output.

During the merging phase, the algorithm calculates the minimum number of shared groups possible. This process leads to a highly compressed representation that is considerably smaller than what an optimal PLA description computed for the same maximum error threshold could achieve.

The provided repository also includes a newer algorithm called Sim-Piece+, which builds upon the original Sim-Piece technique by introducing variable encoding of the PLA coefficients. This enhancement results in an even smaller representation size.

The following graph illustrates the performance of these algorithms on a diverse range of real datasets, comparing them to the optimal PLA representation for the same maximum error bound.

![Relative Compression Ratio](docs/relative_cr.svg)

One possible extension to the algorithm is to optimize the line used to represent a joint group, in addition to optimizing the overall size of the representation given a maximum error threshold. Currently, the code does not explicitly focus on optimizing the line representation within shared segments. Still, the extremely compact representation of Sim-Piece results in smaller Mean Absolute Error (MAE) and Root Mean Squared Error (RMSE) values than alternative PLA techniques for the same space (see our paper for more details). It is possible to adapt the Sim-Piece algorithm to provide even better results regarding these metrics by changing the way the line of each shared segment is produced.

## Test
Execute (Requires Java SE 8+):

```
mvn test -Dtest=TestPLA
```

### Error thresholds 𝜖 (Epsilon)
Results for 0.5% × 𝑟𝑎𝑛𝑔𝑒 ≤ 𝜖 ≤ 5% × 𝑟𝑎𝑛𝑔𝑒

The 𝑟𝑎𝑛𝑔𝑒 is defined as the difference between the maximum and the minimum value of a signal.

### Metrics
- Compression Ratio
- Execution Time


### PLA Methods
- [Mix-Piece](https://link.springer.com/article/10.1007/s00778-024-00862-z) - [PDF file]()
- [Sim-Piece](https://dl.acm.org/doi/10.14778/3594512.3594521) - [PDF file](https://xkitsios.github.io/assets/pdf/simpiece-pvldb23.pdf)
- [Swing](https://dl.acm.org/doi/10.14778/1687627.1687645)
- [PMCMR](https://ieeexplore.ieee.org/document/1260811)


### Datasets

- [Cricket](https://www.cs.ucr.edu/~eamonn/time_series_data_2018)
- [FaceFour](https://www.cs.ucr.edu/~eamonn/time_series_data_2018)
- [Lighting](https://www.cs.ucr.edu/~eamonn/time_series_data_2018)
- [MoteStrain](https://www.cs.ucr.edu/~eamonn/time_series_data_2018)
- [Wafer](https://www.cs.ucr.edu/~eamonn/time_series_data_2018)
- [Wind Speed](https://data.neonscience.org/data-products/DP1.20059.001/RELEASE-2022)
- [Wind Direction](https://data.neonscience.org/data-products/DP1.20059.001/RELEASE-2022)
- [Pressure](https://data.neonscience.org/data-products/DP1.20004.001/RELEASE-2022)
- [BTCUSD]()
- [ETHUSD]()
- [SPX]()
- [STOXX50E]()

### Example Output
>/Cricket.csv.gz  
>Sim-Piece  
>Epsilon: 0.50%	Compression Ratio: 9.379	Execution Time: 194ms  
>Epsilon: 1.00%	Compression Ratio: 16.218	Execution Time: 87ms  
>Epsilon: 1.50%	Compression Ratio: 23.240	Execution Time: 34ms  
>Epsilon: 2.00%	Compression Ratio: 30.485	Execution Time: 28ms  
>Epsilon: 2.50%	Compression Ratio: 37.677	Execution Time: 24ms  
>Epsilon: 3.00%	Compression Ratio: 45.132	Execution Time: 22ms  
>Epsilon: 3.50%	Compression Ratio: 52.724	Execution Time: 19ms  
>Epsilon: 4.00%	Compression Ratio: 60.568	Execution Time: 16ms  
>Epsilon: 4.50%	Compression Ratio: 68.335	Execution Time: 15ms  
>Epsilon: 5.00%	Compression Ratio: 76.724	Execution Time: 14ms  
>Sim-Piece Variable Byte  
>Epsilon: 0.50%	Compression Ratio: 13.650	Execution Time: 70ms  
>Epsilon: 1.00%	Compression Ratio: 23.244	Execution Time: 45ms  
>Epsilon: 1.50%	Compression Ratio: 32.980	Execution Time: 39ms  
>Epsilon: 2.00%	Compression Ratio: 43.047	Execution Time: 34ms  
>Epsilon: 2.50%	Compression Ratio: 52.992	Execution Time: 23ms  
>Epsilon: 3.00%	Compression Ratio: 63.193	Execution Time: 19ms  
>Epsilon: 3.50%	Compression Ratio: 73.646	Execution Time: 17ms  
>Epsilon: 4.00%	Compression Ratio: 84.291	Execution Time: 15ms  
>Epsilon: 4.50%	Compression Ratio: 94.647	Execution Time: 16ms  
>Epsilon: 5.00%	Compression Ratio: 106.225	Execution Time: 13ms  
>Sim-Piece Variable Byte & ZStd  
>Epsilon: 0.50%	Compression Ratio: 14.745	Execution Time: 72ms  
>Epsilon: 1.00%	Compression Ratio: 25.135	Execution Time: 39ms  
>Epsilon: 1.50%	Compression Ratio: 35.686	Execution Time: 32ms  
>Epsilon: 2.00%	Compression Ratio: 46.615	Execution Time: 25ms  
>Epsilon: 2.50%	Compression Ratio: 57.318	Execution Time: 19ms  
>Epsilon: 3.00%	Compression Ratio: 68.463	Execution Time: 18ms  
>Epsilon: 3.50%	Compression Ratio: 79.700	Execution Time: 17ms  
>Epsilon: 4.00%	Compression Ratio: 91.179	Execution Time: 15ms  
>Epsilon: 4.50%	Compression Ratio: 102.446	Execution Time: 13ms  
>Epsilon: 5.00%	Compression Ratio: 114.882	Execution Time: 14ms  
>Swing  
>Epsilon: 0.50%	Compression Ratio: 3.113	Execution Time: 79ms  
>Epsilon: 1.00%	Compression Ratio: 5.361	Execution Time: 37ms  
>Epsilon: 1.50%	Compression Ratio: 7.585	Execution Time: 16ms  
>Epsilon: 2.00%	Compression Ratio: 9.811	Execution Time: 32ms  
>Epsilon: 2.50%	Compression Ratio: 12.054	Execution Time: 30ms  
>Epsilon: 3.00%	Compression Ratio: 14.268	Execution Time: 13ms  
>Epsilon: 3.50%	Compression Ratio: 16.506	Execution Time: 12ms  
>Epsilon: 4.00%	Compression Ratio: 18.677	Execution Time: 13ms  
>Epsilon: 4.50%	Compression Ratio: 20.683	Execution Time: 12ms  
>Epsilon: 5.00%	Compression Ratio: 22.810	Execution Time: 71ms  
>PMCMR  
>Epsilon: 0.50%	Compression Ratio: 2.066	Execution Time: 53ms  
>Epsilon: 1.00%	Compression Ratio: 3.253	Execution Time: 34ms  
>Epsilon: 1.50%	Compression Ratio: 4.541	Execution Time: 31ms  
>Epsilon: 2.00%	Compression Ratio: 5.888	Execution Time: 41ms  
>Epsilon: 2.50%	Compression Ratio: 7.308	Execution Time: 33ms  
>Epsilon: 3.00%	Compression Ratio: 8.791	Execution Time: 15ms  
>Epsilon: 3.50%	Compression Ratio: 10.332	Execution Time: 13ms  
>Epsilon: 4.00%	Compression Ratio: 11.959	Execution Time: 11ms  
>Epsilon: 4.50%	Compression Ratio: 13.659	Execution Time: 16ms  
>Epsilon: 5.00%	Compression Ratio: 15.438	Execution Time: 12ms  >...

## Notes
- It is recommended to use Sim-Piece with Variable Byte encoding.
- Execution time may vary depending on hardware specifications and other factors.

## Who do I talk to?
- [Xenophon Kitsios](https://xkitsios.github.io/)
- [Panagiotis Liakos](https://cgi.di.uoa.gr/~p.liakos/)
- [Katia Papakonstantinopoulou](https://www2.aueb.gr/users/katia/)
- [Yannis Kotidis](https://pages.aueb.gr/users/kotidis/)
