Simple non-linear build pipeline, replicated in several modules

```bash
./mill -i bar.concat
./mill -i bar.compress
./mill -i bar.zipped
./mill -i qux.concat
./mill -i qux.compress
./mill -i qux.zipped
```

```diff
2 - Nonlinear
```