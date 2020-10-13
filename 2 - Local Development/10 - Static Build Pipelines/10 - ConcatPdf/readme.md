Static blog pipeline which can generate PDFs for each blog post and concatenate
them using Apache PDFBox

```bash
./mill -i pdfs
./mill -i combinedPdf
ls out/pdfs/dest | grep '\.pdf'
ls out/combinedPdf/dest | grep '\.pdf'
```


```diff
9 - PostPdf
```