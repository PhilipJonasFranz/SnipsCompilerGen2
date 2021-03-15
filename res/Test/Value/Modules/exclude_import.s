.data
Global.globalVar: .word 10
    
.text
.global Global.foo
Global.foo:                                  /* Function: Global.foo */
    ldr r1, .POOL@-2043634595_0_L0_Global.globalVar/* Evaluate Expression */
    ldr r1, [r1]
    lsl r0, r1, #1
    bx lr
.POOL@-2043634595_0_L0_Global.globalVar: .word Global.globalVar
