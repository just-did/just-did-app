package com.zhouyp.justdid.ui.qrcode

import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.zhouyp.justdid.R

class CustomScannerActivity : CaptureActivity() {

    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(R.layout.custom_scanner)
        return findViewById(R.id.zxing_barcode_scanner)
    }
}
