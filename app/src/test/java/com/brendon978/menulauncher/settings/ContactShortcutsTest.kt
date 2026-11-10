package com.brendon978.menulauncher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactShortcutsTest {
    private fun contato(i: Int) = ContactShortcut("content://contatos/$i", "Pessoa $i")

    @Test fun iniciais() {
        assertEquals("MS", contactInitials("Maria Silva"))
        assertEquals("JS", contactInitials("João da Silva"))
        assertEquals("A", contactInitials("ana"))
        assertEquals("ÉL", contactInitials("  élida   lima "))
        assertEquals("1", contactInitials("190"))
        assertEquals("?", contactInitials(""))
        assertEquals("?", contactInitials("😀"))
    }

    @Test fun paraEmQuatro() {
        val cheia = (1..4).fold(emptyList<ContactShortcut>()) { l, i -> ContactOps.add(l, contato(i)) }
        assertEquals(4, cheia.size)
        assertEquals(cheia, ContactOps.add(cheia, contato(5)))
    }

    @Test fun naoRepete() {
        val l = listOf(contato(1))
        assertEquals(l, ContactOps.add(l, contato(1).copy(name = "Outro nome")))
    }

    @Test fun idaEVoltaGuardaAOrdem() {
        val l = listOf(contato(2), contato(1))
        assertEquals(l, ContactOps.decode(ContactOps.encode(l)))
    }

    @Test fun nomeComQuebraDeLinhaNaoEstragaALista() {
        val l = listOf(ContactShortcut("content://a", "Ana\nMaria\tSilva"), contato(1))
        val volta = ContactOps.decode(ContactOps.encode(l))
        assertEquals(2, volta.size)
        assertEquals("Ana Maria Silva", volta[0].name)
    }

    @Test fun textoRuimOuListaGrandeNaoQuebra() {
        assertEquals(emptyList<ContactShortcut>(), ContactOps.decode(null))
        assertEquals(emptyList<ContactShortcut>(), ContactOps.decode("lixo\n\n"))
        assertNull(ContactShortcut.decode("\tsem endereço"))
        assertEquals(4, ContactOps.decode(ContactOps.encode((1..6).map(::contato))).size)
    }
}
