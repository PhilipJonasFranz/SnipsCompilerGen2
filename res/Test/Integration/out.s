/* --test_08.txt */
.data
arr: 
        .word 5
        .word 8
HEAP_START: .word 0
    
.text
    b main
/* Reseve a memory block with given size + 1. The block size has to be increased */
/* by one to accomodate the block metadata. The function returns a void pointer to */
/* the start of the reserved memory section + 1. This means the pointer points to */
/* the start of the memory thats actually supposed to be used. */
resv:                         /* Function: resv */
    push { r3, r4, r5, r6, r7, r8, r9 }
    mov r3, r0
/* Add block header to size */
    add r3, r3, #1
/* Get reference to heap start */
    ldr r0, .POOL0_L4_HEAP_START/* Evaluate Expression */
    lsr r0, r0, #2
    mov r4, r0
resv.L1:                      /* Evaluate condition */
    lsl r0, r4, #2            /* Convert to bytes */
    ldr r0, [r0]              /* Load from address */
    cmp r0, #0
    beq resv.L2
/* Memory Section is allocated or free */
/* Load block size from memory */
    lsl r0, r4, #2            /* Convert to bytes */
    ldr r0, [r0]              /* Load from address */
    mov r5, r0
    mov r6, r0
    cmp r5, #0
    bge resv.L4
/* Defragment Heap */
resv.L6:                      /* Evaluate condition */
    mov r0, #1
    cmp r0, #1
    bne resv.L7
    sub r0, r4, r5
    lsl r0, r0, #2            /* Convert to bytes */
    ldr r0, [r0]              /* Load from address */
    mov r7, r0
    cmp r7, #0
    ble resv.L9
    b resv.L7
resv.L9: 
    cmp r7, #0
    bne resv.L11
    mov r0, #0                /* Evaluate Expression */
    mov r5, #0
/* All structures to end are free */
    b resv.L7
resv.L11: 
    add r5, r5, r7
/* Add to current size */
    b resv.L6
resv.L7: 
    mov r0, r5                /* Evaluate Expression */
    lsl r1, r4, #2
    str r0, [r1]
/* Write defragmented size to heap */
    mov r7, r0
    cmp r5, #0
    bne resv.L12
    mov r0, #0                /* Evaluate Expression */
    push { r0 }
    add r0, r4, r3
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
resv.L12: 
/* Memory location is free, check size */
    rsb r0, r5, #0
    mov r5, r0
    cmp r0, r3
    bne resv.L15
/* Fits */
    mov r0, r3                /* Evaluate Expression */
    lsl r1, r4, #2
    str r0, [r1]
    mov r8, r0                /* Evaluate Expression */
    mov r1, r4
    mov r2, #1
    add r0, r1, #1
    b resv.L18
resv.L15: 
    cmp r5, r3
    ble resv.L17
/* Rest, subtract size, store at offset */
    sub r5, r5, r3
    mov r0, r3                /* Evaluate Expression */
    lsl r1, r4, #2
    str r0, [r1]
    mov r9, r0                /* Evaluate Expression */
    rsb r0, r5, #0
    push { r0 }
    add r0, r4, r3
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
    mov r1, r4                /* Evaluate Expression */
    mov r2, #1
    add r0, r1, #1
    b resv.L18
resv.L17: 
    add r4, r4, r5
    b resv.L3
resv.L4: 
/* Memory section is allocated, skip */
    add r4, r4, r5
resv.L3: 
    b resv.L1
resv.L2: 
/* End reached, create new block */
    mov r0, r3                /* Evaluate Expression */
    lsl r1, r4, #2
    str r0, [r1]
    mov r5, r0                /* Evaluate Expression */
    mov r1, r4
    mov r2, #1
    add r0, r1, #1
resv.L18: 
    pop { r3, r4, r5, r6, r7, r8, r9 }
    bx lr
.POOL0_L4_HEAP_START: .word HEAP_START
    
/* Frees the memory block the given pointer points to. The pointer object will keep */
/* the memory address, but the memory block will not be registered to be used anymore and */
/* thus can be overwritten. */
free:                         /* Function: free */
    push { r3, r4, r5, r6, r7, r8, r9 }
    mov r3, r0
/* Jump to block head */
    sub r3, r3, #1
/* Load Block Size */
    lsl r0, r3, #2            /* Convert to bytes */
    ldr r0, [r0]              /* Load from address */
    mov r4, r0
/* Store negated block size */
    mov r5, r0                /* Evaluate Expression */
    rsb r0, r4, #0
    lsl r1, r3, #2
    str r0, [r1]
    mov r6, r0                /* Evaluate Expression */
    add r7, r3, r4
free.L1:                      /* Evaluate condition */
    lsl r0, r7, #2            /* Convert to bytes */
    ldr r0, [r0]              /* Load from address */
    cmp r0, #0
    bge free.L2
    lsl r0, r7, #2            /* Convert to bytes */
    ldr r0, [r0]              /* Load from address */
    mov r8, r0
/* Add size to freed block */
    mov r9, r0                /* Evaluate Expression */
    lsl r0, r3, #2            /* Convert to bytes */
    ldr r0, [r0]              /* Load from address */
    mov r1, r0
    add r0, r1, r8
    lsl r1, r3, #2
    str r0, [r1]
/* Shift pointer to next block */
    sub r7, r7, r8
    b free.L1
free.L2: 
    pop { r3, r4, r5, r6, r7, r8, r9 }
    bx lr
    
/* A single list entry, contains a pointer to the next node */
/* and a value. */
/* Wraps the list head and tail. */
/* Creates a new List Object and initializes the head an tail to 0. */
List.create_P0:               /* Function: List.create, Provisos: INT */
    push { r3, fp, lr }
    mov fp, sp
    mov r0, #2                /* Evaluate Expression */
    bl resv                   /* Call resv */
    mov r3, r0
/* Initialize list, set pointers to 0 */
    mov r0, #0                /* Evaluate Expression */
    push { r0 }
    mov r0, #0
    lsl r1, r3, #2
    str r0, [r1, #0]
    pop { r0 }
    str r0, [r1, #4]
    mov r0, r3                /* Evaluate Expression */
    mov sp, fp
    pop { r3, fp, lr }
    bx lr
    
/* Free the list by freeing all contained list nodes, and the list itself. */
List.destroy:                 /* Function: List.destroy */
    push { r3, r4, fp, lr }
    mov fp, sp
    mov r3, r0
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r4, r0
List.destroy.L1:              /* Evaluate condition */
    cmp r4, #0
    beq List.destroy.L2
    mov r0, r4                /* Load parameters */
    bl free                   /* Call free */
    lsl r1, r4, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r4, r0
    b List.destroy.L1
List.destroy.L2: 
    mov r0, r3                /* Load parameters */
    bl free                   /* Call free */
    mov sp, fp
    pop { r3, r4, fp, lr }
    bx lr
    
/* Creates a new list node for given value x and adds the node at the */
/* end of the list. */
List.add_P1:                  /* Function: List.add, Provisos: INT */
    push { r3, r4, r5, r6, r7, fp, lr }
    mov fp, sp
    mov r3, r0
    mov r4, r1
    mov r0, #2                /* Evaluate Expression */
    bl resv                   /* Call resv */
    mov r5, r0
/* Initialize the new List Node, set the next pointer to null */
    mov r0, r4                /* Evaluate Expression */
    push { r0 }
    mov r6, r0
    mov r0, #0
    lsl r1, r5, #2
    str r0, [r1, #0]
    pop { r0 }
    str r0, [r1, #4]
    mov r7, r0
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    cmp r0, #0
    bne List.add.L1
    mov r0, r5                /* Evaluate Expression */
    lsl r1, r3, #2            /* Convert to bytes */
    str r0, [r1]              /* Store value to struct field */
/* Dereference through pointer */
    mov r0, r5                /* Evaluate Expression */
    lsl r1, r3, #2            /* Convert to bytes */
    add r1, r1, #4
    str r0, [r1]              /* Store value to struct field */
    b List.add.L0
List.add.L1: 
    mov r0, r5                /* Evaluate Expression */
    lsl r1, r3, #2            /* Convert to bytes */
    add r1, r1, #4
    ldr r1, [r1]
    lsl r1, r1, #2
    str r0, [r1]              /* Store value to struct field */
    mov r0, r5                /* Evaluate Expression */
    lsl r1, r3, #2            /* Convert to bytes */
    add r1, r1, #4
    str r0, [r1]              /* Store value to struct field */
List.add.L0: 
    mov sp, fp
    pop { r3, r4, r5, r6, r7, fp, lr }
    bx lr
    
/* Creates a new list node for given value x and adds the node at the */
/* end of the list. */
List.get_P2:                  /* Function: List.get, Provisos: INT */
    push { r3, r4, r5 }
    mov r3, r0
    mov r4, r1
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r5, r0
List.get.L1:                  /* Evaluate condition */
    cmp r4, #0
    beq List.get.L2
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    cmp r0, #0
    bne List.get.L3
    mov r0, #0                /* Evaluate Expression */
    b List.get.L5
List.get.L3: 
/* Jump to next */
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r5, r0
    mov r0, r4
    sub r4, r0, #1
    b List.get.L1
List.get.L2: 
    lsl r1, r5, #2            /* Convert to bytes */
    add r1, r1, #4
    ldr r0, [r1]              /* Load field from struct */
List.get.L5: 
    pop { r3, r4, r5 }
    bx lr
    
/* Check if given list contains given value. */
    
/* Finds the first node in the list that has given value. */
/* Returns a pointer to this list node, or 0 in case the */
/* value wasnt found. Frees the removed node of the heap. */
    
/* Returns the amount of elements stored in the list. */
List.size:                    /* Function: List.size */
    push { r3, r4, r5 }
    mov r3, r0
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r4, r0
    mov r0, #0                /* Evaluate Expression */
    mov r5, #0
List.size.L1:                 /* Evaluate condition */
    cmp r4, #0
    beq List.size.L2
    lsl r1, r4, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r4, r0
    mov r0, r5
    add r5, r0, #1
    b List.size.L1
List.size.L2: 
    mov r0, r5                /* Evaluate Expression */
    pop { r3, r4, r5 }
    bx lr
    
/* Removes the index at given index. */
/* If the index is out of bounds nothing is removed. */
List.remove:                  /* Function: List.remove */
    push { r3, r4, r5, r6, r7, r8, r9, fp, lr }
    mov fp, sp
    mov r3, r0
    mov r4, r1
/* Out of bounds */
    cmp r4, #0
    bge List.remove.L0
    b List.remove.L17
List.remove.L0: 
    cmp r4, #0
    bne List.remove.L3
/* Remove first element */
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    cmp r0, #0
    beq List.remove.L4
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r5, r0
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r1, [r1]
    lsl r1, r1, #2
    ldr r0, [r1]              /* Load field from struct */
    lsl r1, r3, #2            /* Convert to bytes */
    str r0, [r1]              /* Store value to struct field */
/* Only one element in list */
    lsl r1, r3, #2            /* Convert to bytes */
    add r1, r1, #4
    ldr r0, [r1]              /* Load field from struct */
    cmp r0, r5
    bne List.remove.L6
    mov r0, #0                /* Evaluate Expression */
    lsl r1, r3, #2            /* Convert to bytes */
    add r1, r1, #4
    str r0, [r1]              /* Store value to struct field */
List.remove.L6: 
    mov r0, r5                /* Load parameters */
    bl free                   /* Call free */
List.remove.L4: 
    b List.remove.L17
List.remove.L3: 
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r5, r0
    mov r0, r4
    sub r4, r0, #1
/* Jump to element before element to remove */
List.remove.L9:               /* Evaluate condition */
    mov r6, r0
    cmp r4, #0
    ble List.remove.L10
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    cmp r0, #0
    bne List.remove.L12
    b List.remove.L17
List.remove.L12: 
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r5, r0
    mov r0, r4
    sub r4, r0, #1
    b List.remove.L9
List.remove.L10: 
/* Out of bounds */
    mov r7, r0
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    cmp r0, #0
    bne List.remove.L14
    b List.remove.L17
List.remove.L14: 
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r1, [r1]
    lsl r1, r1, #2
    ldr r0, [r1]              /* Load field from struct */
    cmp r0, #0
    bne List.remove.L16
/* Element to remove is last element in list */
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    bl free                   /* Call free */
    mov r0, #0                /* Evaluate Expression */
    lsl r1, r5, #2            /* Convert to bytes */
    str r0, [r1]              /* Store value to struct field */
    mov r0, r5                /* Evaluate Expression */
    lsl r1, r3, #2            /* Convert to bytes */
    add r1, r1, #4
    str r0, [r1]              /* Store value to struct field */
    b List.remove.L15
List.remove.L16: 
/* Cut out element and set next pointers */
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r1, [r1]
    lsl r1, r1, #2
    ldr r0, [r1]              /* Load field from struct */
    mov r8, r0
    mov r9, r0                /* Load parameters */
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    bl free                   /* Call free */
    mov r0, r8                /* Evaluate Expression */
    lsl r1, r5, #2            /* Convert to bytes */
    str r0, [r1]              /* Store value to struct field */
List.remove.L15: 
List.remove.L17: 
    mov sp, fp
    pop { r3, r4, r5, r6, r7, r8, r9, fp, lr }
    bx lr
    
GameObject.Items.set:         /* Function: GameObject.Items.set */
    push { r3, r4, r5, r6 }
    mov r3, r0
    mov r4, r1
    mov r5, r2
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r6, r0
    mov r0, r5                /* Evaluate Expression */
    push { r0 }
    add r0, r6, r4
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
    pop { r3, r4, r5, r6 }
    bx lr
    
GameObject.Items.get:         /* Function: GameObject.Items.get */
    push { r3, r4, r5 }
    mov r3, r0
    mov r4, r1
    lsl r1, r3, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    mov r5, r0
    add r0, r5, r4
    lsl r0, r0, #2            /* Convert to bytes */
    ldr r0, [r0]              /* Load from address */
    pop { r3, r4, r5 }
    bx lr
    
/* A tuple containing two different value types */
/* A contianer containing a value */
/* Inital Player data */
main:                         /* Function: main */
    push { r3, r4, r5, r6, fp, lr }
    mov fp, sp
/* Initialize Inventory with size 10 */
    mov r0, #1                /* Evaluate Expression */
    bl resv                   /* Call resv */
    mov r3, r0
    mov r0, #10               /* Evaluate Expression */
    bl resv                   /* Call resv */
    lsl r1, r3, #2
    str r0, [r1, #0]
/* Initialize Player Object */
    mov r4, r0                /* Evaluate Expression */
    mov r0, #3
    bl resv                   /* Call resv */
    mov r5, r0
    mov r0, r3                /* Evaluate Expression */
    push { r0 }
    ldr r0, .POOL1_L3_arr     /* Load data section address */
    add r0, r0, #4
    ldr r0, [r0]
    push { r0 }
    ldr r0, .POOL1_L3_arr     /* Load data section address */
    add r0, r0, #0
    ldr r0, [r0]
    lsl r1, r5, #2
    str r0, [r1, #0]
    pop { r0 }
    str r0, [r1, #4]
    pop { r0 }
    str r0, [r1, #8]
    lsl r1, r5, #2            /* Convert to bytes */
    add r1, r1, #4
    ldr r0, [r1]              /* Load field from struct */
    push { r0 }
    lsl r1, r5, #2            /* Convert to bytes */
    ldr r0, [r1]              /* Load field from struct */
    push { r0 }
    mov r0, #1                /* Evaluate Expression */
    push { r0 }
    mov r0, #3
    mov r1, #1
    push { r1, r0 }
    sub r1, fp, #20           /* Evaluate Expression */
    ldr r0, [r1]              /* Load field from struct */
    push { r0 }
    sub r1, fp, #20           /* Load field location */
    add r1, r1, #4
    ldr r0, [r1]              /* Load field from struct */
    mov r2, r0
    pop { r1 }
    add r0, r1, r2
    push { r0 }
    sub r1, fp, #20           /* Load field location */
    add r1, r1, #8
    ldr r0, [r1]              /* Load field from struct */
    mov r2, r0
    pop { r1 }
    add r0, r1, r2
    push { r0 }
    ldr r0, [fp, #-24]        /* Evaluate Expression */
    push { r0 }
    mov r0, r5
    push { r0 }
/* Set item at index 1 to 5 */
    sub r1, fp, #24           /* Load parameters */
    ldr r0, [r1]              /* Load field from struct */
    push { r0 }
    mov r0, #1
    push { r0 }
    lsl r1, r5, #2            /* Convert to bytes */
    add r1, r1, #8
    ldr r0, [r1]              /* Load field from struct */
    pop { r1, r2 }
    bl GameObject.Items.set   /* Call GameObject.Items.set */
    bl List.create_P0         /* Evaluate Expression */
    mov r6, r0
    sub r1, fp, #8            /* Load parameters */
    ldr r0, [r1]              /* Load field from struct */
    push { r0 }
    mov r0, r6
    pop { r1 }
    bl List.add_P1            /* Call List.add */
    sub r1, fp, #8            /* Load parameters */
    add r1, r1, #4
    ldr r0, [r1]              /* Load field from struct */
    push { r0 }
    mov r0, r6
    pop { r1 }
    bl List.add_P1            /* Call List.add */
    mov r0, #0                /* Load parameters */
    push { r0 }
    mov r0, r6
    pop { r1 }
    bl List.remove            /* Call List.remove */
    mov r0, #1                /* Evaluate Expression */
    push { r0 }
    sub r1, fp, #32           /* Load field location */
    ldr r1, [r1]
    lsl r1, r1, #2
    add r1, r1, #8
    ldr r0, [r1]              /* Load field from struct */
    pop { r1 }
    bl GameObject.Items.get   /* Call GameObject.Items.get */
    push { r0 }
    mov r0, #0                /* Load parameters */
    push { r0 }
    mov r0, r6
    pop { r1 }
    bl List.get_P2            /* Call List.get */
    mov r2, r0
    pop { r1 }
    add r0, r1, r2
    push { r0 }
    sub r1, fp, #32           /* Load field location */
    add r1, r1, #4
    ldr r0, [r1]              /* Load field from struct */
    mov r2, r0
    pop { r1 }
    add r0, r1, r2
    mov sp, fp
    pop { r3, r4, r5, r6, fp, lr }
    bx lr
.POOL1_L3_arr: .word arr
    