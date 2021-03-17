.version 2597775093

.text
.global hsize
hsize:                                       /* Function: hsize */
    push { r3 }
    mov r3, r0
    sub r3, r0, #1
    ldr r1, [r10, r3, lsl #2]                /* Load from address */
    sub r0, r1, #1
    pop { r3 }
    bx lr
