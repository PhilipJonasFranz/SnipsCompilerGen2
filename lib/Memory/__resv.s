__resv:
	add r0, r0, #4  // Add one byte for header cell
	push {r1, r2, r3}
	ldr r1, a_heap_start // Depends on heap start
	ldr r1, [r1] // Load heap start
__resv_loop:
	ldr r2, [r1]
	cmp r2, #0 // Check if cell is zero, end reached
	bne __resv_not_zero // Jump to next cell
	str r0, [r1] // Allocate and return
	b __resv_end
__resv_not_zero:
	bgt __resv_not_negative // Jump if cell is positive
	rsb r3, r0, #0 // Negate block size cell
	cmp r3, r0
	blt __resv_not_zero_low // Cell is bigger than needed
	add r3, r2, r0
	str r0, [r1]
	cmp r3, #0
	bne __resv_not_zero_rest
	b __resv_end
__resv_not_zero_rest:
	add r2, r1, r0
	str r3, [r2]
	b __resv_end
__resv_not_zero_low:
	add r1, r1, r3
	b __resv_loop
__resv_not_negative:
	add r1, r1, r2
	b __resv_loop
__resv_end:
	mov r0, r1
	pop {r1, r2, r3}
	bx lr
