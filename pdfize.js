#!/usr/bin/env node

const puppeteer = require('puppeteer');

const readline = require('readline');
const pageBuffer = []
puppeteer.launch().then(browser => {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout,
        terminal: false
    });

    rl.on('line', async function(line){
        const [key, arg1, arg2, narrowedMarginMM] = JSON.parse(line);
        const page = (pageBuffer.length === 0) ? await browser.newPage() : pageBuffer.pop();
        try {
            page.emulateMedia("screen");
            await page.goto("file://" + arg1, {waitUntil: 'load'});
            const sevenInchInMM = 177.8
            const nineInchInMM = 228.6
            const desiredMarginMM = 15.5
            const contentHeightMM = nineInchInMM - desiredMarginMM * 2
            const contentWidthMM = sevenInchInMM - desiredMarginMM * 2
            const narrowedBottomMarginMM = narrowedMarginMM + 5
            await page.pdf({
                path: arg2,
                // format: 'A4',
                width: contentWidthMM + narrowedMarginMM * 2 + "mm",
                height: contentHeightMM + narrowedMarginMM + narrowedBottomMarginMM + "mm",
                scale: 0.79,
                margin: {
                    top: narrowedMarginMM + "mm",
                    bottom: narrowedBottomMarginMM + "mm",
                    left: narrowedMarginMM + "mm",
                    right: narrowedMarginMM + "mm",
                },
                printBackground: true
            });

            console.log("Done " + key);
        }finally{
            pageBuffer.push(page);
        }
        // console.log(line);
    })

    console.log("Spawned")

})
