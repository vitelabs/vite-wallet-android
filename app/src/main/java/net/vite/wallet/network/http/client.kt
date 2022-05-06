package net.vite.wallet.network.http

import android.util.Base64
import net.vite.wallet.utils.toHex
import org.vitelabs.mobile.Mobile
import kotlin.experimental.xor


fun abc(): ByteArray {
    val p1 = Int.MAX_VALUE.toString(16)
    val p2 = "___+1231il1ll1l2l31l1l13,,,,,,"
    val p3 = hamlet1
    return Mobile.hash(32, (p1 + p2 + p3).toByteArray())
}

fun cde(): ByteArray {
    val p1 = Int.MIN_VALUE.toString(16)
    val p2 = "01o2z0OOO11OOO10O111OO1"
    val p3 = hamlet1
    return Mobile.hash(32, (p1 + p2 + p3).toByteArray())
}

fun eee(): ByteArray {
    val a = abc()
    val b = cde()
    val c = ByteArray(32)
    c.fill(0, 0, c.size - 1)
    a.forEachIndexed { index, value ->
        c[index] = value.xor(b[index])
    }
    return c
}

fun ctrEncrypt(a: String): String {
    val a1 = cde()
    val b1 = abc()
    a1.toHex()
    b1.toHex()
    val s = Mobile.hash(32, eee())
    val key = s.sliceArray(0 until 16)
    val iv = s.sliceArray(16 until 32)
    val s1 = Mobile.aesCTRXOR(key, a.toByteArray(), iv)
    return Base64.encodeToString(s1, Base64.NO_WRAP or Base64.NO_PADDING)
}


private const val hamlet1 = "Hi ! ViteBabe? To be, or not to be, that is the question:\n" +
        "Whether 'tis nobler in the mind to suffer\n" +
        "The slings and arrows of outrageous fortune,\n" +
        "Or to take Arms against a Sea of troubles,\n" +
        "And by opposing end them: to die, to sleep\n" +
        "No more; and by a sleep, to say we end\n" +
        "The heart-ache, and the thousand natural shocks\n" +
        "That Flesh is heir to? 'Tis a consummation\n" +
        "Devoutly to be wished. To die, to sleep,\n" +
        "To sleep, perchance to Dream; aye, there's the rub,\n" +
        "For in that sleep of death, what dreams may come,\n" +
        "When we have shuffled off this mortal coil,\n" +
        "Must give us pause. There's the respect\n" +
        "That makes Calamity of so long life:\n" +
        "For who would bear the Whips and Scorns of time,\n" +
        "The Oppressor's wrong, the proud man's Contumely, \n" +
        "The pangs of despised Love, the Law’s delay, \n" +
        "The insolence of Office, and the spurns\n" +
        "That patient merit of the unworthy takes,\n" +
        "When he himself might his Quietus make\n" +
        "With a bare Bodkin? Who would Fardels bear,\n" +
        "To grunt and sweat under a weary life,\n" +
        "But that the dread of something after death,\n" +
        "The undiscovered country, from whose bourn\n" +
        "No traveller returns, puzzles the will,\n" +
        "And makes us rather bear those ills we have,\n" +
        "Than fly to others that we know not of.\n" +
        "Thus conscience does make cowards of us all,\n" +
        "And thus the native hue of Resolution\n" +
        "Is sicklied o'er, with the pale cast of Thought,\n" +
        "And enterprises of great pitch and moment, [F: pith]\n" +
        "With this regard their Currents turn awry, [F: away]\n" +
        "And lose the name of Action. Soft you now,\n" +
        "The fair Ophelia? Nymph, in thy Orisons\n" +
        "Be all my sins remember'd."
