.data
Tree.TreeNode: .word 0
    
.text
    
.global Tree.TreeNode.insert_P_1
Tree.TreeNode.insert_P_1:                    /* Function: Tree.TreeNode.insert, Provisos: INT */
    sub sp, sp, #28
    stmea sp, {r3-r8, lr}
    mov r3, r0
    mov r4, r1
    mov r5, r2
    push { r5 }
    ldr r0, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    stmfd sp!, {r0, r1}
    ldr r0, .POOL@-1914853343_0_Tree.TreeNode
    push { r0 }
    bl init_P_4                              /* Call init */
    mov r6, r0
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    cmp r3, r1
    bne Tree.TreeNode.insert.L1
    mov r0, r6                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8, pc}
Tree.TreeNode.insert.L1: 
    mov r0, r3                               /* Evaluate Expression */
    mov r7, r3
Tree.TreeNode.insert.L4:                     /* Evaluate condition */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    cmp r7, r1
    ldmfdeq sp!, {r3-r8, pc}
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r0, [r1, #12]                        /* Load field from struct */
    mov r1, r5
    add lr, pc, #8
    mov pc, r4
    mov r8, r0
    cmp r8, #0
    bne Tree.TreeNode.insert.L7
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8, pc}
Tree.TreeNode.insert.L7: 
    cmp r8, #1
    bne Tree.TreeNode.insert.L8
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    cmp r0, r1
    bne Tree.TreeNode.insert.L10
    lsl r1, r7, #2                           /* Convert to bytes */
    str r6, [r1, #4]                         /* Store value to struct field */
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8, pc}
Tree.TreeNode.insert.L10: 
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r7, [r1, #4]                         /* Load field from struct */
    b Tree.TreeNode.insert.L6
Tree.TreeNode.insert.L8: 
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    cmp r0, r1
    bne Tree.TreeNode.insert.L14
    lsl r1, r7, #2                           /* Convert to bytes */
    str r6, [r1, #8]                         /* Store value to struct field */
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8, pc}
Tree.TreeNode.insert.L14: 
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r7, [r1, #8]                         /* Load field from struct */
Tree.TreeNode.insert.L6: 
    b Tree.TreeNode.insert.L4
    ldmfd sp!, {r3-r8, pc}
    
.global Tree.TreeNode.create_P_1
Tree.TreeNode.create_P_1:                    /* Function: Tree.TreeNode.create, Provisos: INT */
    push { r3, lr }
    mov r3, r0
    push { r3 }
    ldr r0, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    stmfd sp!, {r0, r1}
    ldr r0, .POOL@-1914853343_0_Tree.TreeNode
    push { r0 }
    bl init_P_4                              /* Call init */
    ldmfd sp!, {r3, pc}
.POOL@-1914853343_0_Tree.TreeNode: .word Tree.TreeNode
.POOL@-1914853343_0_L1_NULL: .word NULL
