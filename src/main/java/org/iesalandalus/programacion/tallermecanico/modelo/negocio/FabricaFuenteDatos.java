package org.iesalandalus.programacion.tallermecanico.modelo.negocio;

import org.iesalandalus.programacion.tallermecanico.modelo.negocio.ficheros.xml.FuenteDatosFicherosXML;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.json.FuenteDatosFicherosJSON;

public enum FabricaFuenteDatos {

    FICHEROS_XML {
        @Override
        public IFuenteDatos crear() {
            return new FuenteDatosFicherosXML();
        }
    },

    FICHEROS_JSON {
        @Override
        public IFuenteDatos crear() {
            return new FuenteDatosFicherosJSON();
        }
    };

    public abstract IFuenteDatos crear();
}
