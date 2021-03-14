.data
List.LinkedList: .word 0
    
List.ListNode: .word 0
    
.text
    
.global List.LinkedList.add_P_1
List.LinkedList.add_P_1:                     /* Function: List.LinkedList.add, Provisos: INT */
    mov r10, #0
    sub sp, sp, #16
    stmea sp, {r3-r5, lr}
    mov r3, r0
    mov r4, r1
    push { r4 }
    ldr r0, .POOL0_L2_NULL                   /* Load null address */
    push { r0 }
    ldr r0, .POOL0_List.ListNode
    push { r0 }
    bl init_P_3                              /* Call init */
    mov r5, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .POOL0_L2_NULL                   /* Load null address */
    cmp r0, r1
    bne List.LinkedList.add.L1
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #4]                         /* Store value to struct field */
    mov r0, r5                               /* Evaluate Expression */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #8]                         /* Store value to struct field */
    b List.LinkedList.add.L0
List.LinkedList.add.L1: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #8]
    lsl r1, r1, #2
    str r5, [r1, #4]                         /* Store value to struct field */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r5, [r1, #8]                         /* Store value to struct field */
List.LinkedList.add.L0: 
    lsl r1, r3, #2                           /* Convert to bytes */
    add r1, r1, #12
    ldr r0, [r1]
    add r2, r0, #1
    str r2, [r1]
    ldmfd sp!, {r3-r5, pc}
.POOL0_List.ListNode: .word List.ListNode
.POOL0_L2_NULL: .word NULL
    
.global List.LinkedList.get_P_1
List.LinkedList.get_P_1:                     /* Function: List.LinkedList.get, Provisos: INT */
    mov r10, #0
    sub sp, sp, #12
    stmea sp, {r3-r5}
    mov r3, r0
    mov r4, r1
    cmp r1, #0
    bge List.LinkedList.get.L0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #16]                        /* Load field from struct */
    b List.LinkedList.get.L7
List.LinkedList.get.L0: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r5, [r1, #4]                         /* Load field from struct */
List.LinkedList.get.L3:                      /* Evaluate condition */
    cmp r4, #0
    beq List.LinkedList.get.L4
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .POOL1_L2_NULL                   /* Load null address */
    cmp r0, r1
    bne List.LinkedList.get.L5
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #16]                        /* Load field from struct */
    b List.LinkedList.get.L7
List.LinkedList.get.L5: 
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r5, [r1, #4]                         /* Load field from struct */
    mov r0, r4
    sub r4, r4, #1
    b List.LinkedList.get.L3
List.LinkedList.get.L4: 
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
List.LinkedList.get.L7: 
    ldmfd sp!, {r3-r5}
    bx lr
.POOL1_L2_NULL: .word NULL
    
.global List.LinkedList.create_P_1
List.LinkedList.create_P_1:                  /* Function: List.LinkedList.create, Provisos: INT */
    push { r3, lr }
    mov r3, r0
    push { r3 }
    mov r0, #0
    ldr r1, .POOL2_L2_NULL                   /* Load null address */
    ldr r2, .POOL2_L2_NULL                   /* Load null address */
    stmfd sp!, {r0-r2}
    ldr r0, .POOL2_List.LinkedList
    push { r0 }
    bl init_P_5                              /* Call init */
    ldmfd sp!, {r3, pc}
.POOL2_List.LinkedList: .word List.LinkedList
.POOL2_L2_NULL: .word NULL
